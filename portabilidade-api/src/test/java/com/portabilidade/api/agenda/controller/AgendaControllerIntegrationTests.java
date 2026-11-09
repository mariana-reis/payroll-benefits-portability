package com.portabilidade.api.agenda.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.portabilidade.api.autorizacao.entity.Autorizacao;
import com.portabilidade.api.autorizacao.repository.AutorizacaoRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class AgendaControllerIntegrationTests {

	private static final WireMockServer wireMock = iniciarWireMock();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AutorizacaoRepository autorizacaoRepository;

	@DynamicPropertySource
	static void centralProperties(DynamicPropertyRegistry registry) {
		registry.add("central.base-url", wireMock::baseUrl);
		registry.add("central.connect-timeout", () -> "2s");
		registry.add("central.read-timeout", () -> "2s");
		registry.add("central.hmac-secret", () -> "test-secret");
		registry.add("central.retry.max-attempts", () -> "3");
		registry.add("central.retry.initial-backoff", () -> "1ms");
		registry.add("central.retry.max-backoff", () -> "5ms");
	}

	@AfterAll
	static void pararWireMock() {
		wireMock.stop();
	}

	@BeforeEach
	void limparEstado() {
		wireMock.resetAll();
		autorizacaoRepository.deleteAll();
	}

	@Test
	void deveRetornarDuasParcelasBEN001() throws Exception {
		autorizar("BEN001");
		wireMock.stubFor(get(urlEqualTo("/beneficiarios/BEN001/agenda"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("""
								[
								  {"parcelaId":"P001","convenioId":"CONV01","valor":150.0,"dataDesconto":"2026-01-10","status":"ABERTA"},
								  {"parcelaId":"P002","convenioId":"CONV01","valor":150.0,"dataDesconto":"2026-02-10","status":"ABERTA"}
								]
								""")));

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/portabilidade/agenda/BEN001"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.parcelas.length()", is(2)))
				.andExpect(jsonPath("$.parcelas[0].parcelaId", is("P001")))
				.andExpect(jsonPath("$.parcelas[0].convenioId", is("CONV01")))
				.andExpect(jsonPath("$.parcelas[0].valor", is(150.0)))
				.andExpect(jsonPath("$.parcelas[0].dataDesconto", is("2026-01-10")))
				.andExpect(jsonPath("$.parcelas[0].status", is("ABERTA")));

		wireMock.verify(1, getRequestedFor(urlEqualTo("/beneficiarios/BEN001/agenda")));
	}

	@Test
	void deveRetornarUmaParcelaBEN002() throws Exception {
		autorizar("BEN002");
		wireMock.stubFor(get(urlEqualTo("/beneficiarios/BEN002/agenda"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("""
								[{"parcelaId":"P010","convenioId":"CONV02","valor":300.0,"dataDesconto":"2026-01-15","status":"ABERTA"}]
								""")));

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/portabilidade/agenda/BEN002"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.parcelas.length()", is(1)))
				.andExpect(jsonPath("$.parcelas[0].parcelaId", is("P010")))
				.andExpect(jsonPath("$.parcelas[0].convenioId", is("CONV02")))
				.andExpect(jsonPath("$.parcelas[0].valor", is(300.0)))
				.andExpect(jsonPath("$.parcelas[0].dataDesconto", is("2026-01-15")))
				.andExpect(jsonPath("$.parcelas[0].status", is("ABERTA")));
	}

	@Test
	void deveRetornar409SemAutorizacao() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/portabilidade/agenda/BEN001")
						.header("X-Correlation-Id", "corr-sem-autorizacao"))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title", is("Autorização obrigatória")))
				.andExpect(jsonPath("$.status", is(409)))
				.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("autorizar")))
				.andExpect(jsonPath("$.timestamp").isNotEmpty())
				.andExpect(jsonPath("$.correlationId", is("corr-sem-autorizacao")));

		wireMock.verify(0, getRequestedFor(urlEqualTo("/beneficiarios/BEN001/agenda")));
	}

	@Test
	void deveRetornar503QuandoCentralIndisponivel() throws Exception {
		autorizar("BEN_ERRO");
		wireMock.stubFor(get(urlEqualTo("/beneficiarios/BEN_ERRO/agenda"))
				.willReturn(aResponse()
						.withStatus(503)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"erro\":\"Central indisponível\"}")));

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/portabilidade/agenda/BEN_ERRO"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title", is("Central indisponível")))
				.andExpect(jsonPath("$.status", is(503)))
				.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Central")))
				.andExpect(jsonPath("$.timestamp").isNotEmpty());
	}

	@Test
	void deveRetornarAgendaVazia() throws Exception {
		autorizar("BEN099");
		wireMock.stubFor(get(urlEqualTo("/beneficiarios/BEN099/agenda"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("[]")));

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/portabilidade/agenda/BEN099"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.parcelas").isArray())
				.andExpect(jsonPath("$.parcelas.length()", is(0)));
	}

	private void autorizar(String beneficiarioId) {
		autorizacaoRepository.saveAndFlush(new Autorizacao(beneficiarioId, "auth-" + beneficiarioId));
	}

	private static WireMockServer iniciarWireMock() {
		WireMockServer server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
		server.start();
		return server;
	}
}

package com.portabilidade.api.autorizacao.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
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
class OptInControllerIntegrationTests {

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
	void deveRegistrarOptInBEN001() throws Exception {
		stubAutorizacao("auth-ben001");

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/portabilidade/opt-in/BEN001"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.beneficiarioId", is("BEN001")))
				.andExpect(jsonPath("$.autorizacaoId", is("auth-ben001")))
				.andExpect(jsonPath("$.criadoEm").isNotEmpty());

		org.assertj.core.api.Assertions.assertThat(autorizacaoRepository.findByBeneficiarioId("BEN001"))
				.isPresent();
		wireMock.verify(1, postRequestedFor(urlEqualTo("/autorizacoes")));
	}

	@Test
	void deveRegistrarOptInBEN002() throws Exception {
		stubAutorizacao("auth-ben002");

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/portabilidade/opt-in/BEN002"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.beneficiarioId", is("BEN002")))
				.andExpect(jsonPath("$.autorizacaoId", is("auth-ben002")))
				.andExpect(jsonPath("$.criadoEm").isNotEmpty());

		org.assertj.core.api.Assertions.assertThat(autorizacaoRepository.findByBeneficiarioId("BEN002"))
				.isPresent();
	}

	@Test
	void deveReutilizarAutorizacaoExistente() throws Exception {
		autorizacaoRepository.saveAndFlush(new Autorizacao("BEN001", "auth-local"));

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/portabilidade/opt-in/BEN001"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.beneficiarioId", is("BEN001")))
				.andExpect(jsonPath("$.autorizacaoId", is("auth-local")))
				.andExpect(jsonPath("$.criadoEm").isNotEmpty());

		org.assertj.core.api.Assertions.assertThat(autorizacaoRepository.count()).isEqualTo(1);
		wireMock.verify(0, postRequestedFor(urlEqualTo("/autorizacoes")));
	}

	@Test
	void deveFalharQuandoCentralIndisponivel() throws Exception {
		wireMock.stubFor(post(urlEqualTo("/autorizacoes"))
				.willReturn(aResponse()
						.withStatus(503)
						.withHeader("Content-Type", "application/json")
						.withBody("""
								{"erro":"Central indisponível no momento (simulado). Tente novamente."}
								""")));

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/portabilidade/opt-in/BEN_ERRO"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title", is("Central indisponível")));

		org.assertj.core.api.Assertions.assertThat(autorizacaoRepository.count()).isZero();
	}

	@Test
	void deveMapear404InesperadoDaCentralComoBadGateway() throws Exception {
		wireMock.stubFor(post(urlEqualTo("/autorizacoes"))
				.willReturn(aResponse().withStatus(404)));

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/portabilidade/opt-in/BEN_UNKNOWN"))
				.andExpect(status().isBadGateway())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title", is("Falha na comunicação com a Central")));

		org.assertj.core.api.Assertions.assertThat(autorizacaoRepository.count()).isZero();
	}

	private void stubAutorizacao(String autorizacaoId) {
		wireMock.stubFor(post(urlEqualTo("/autorizacoes"))
				.willReturn(aResponse()
						.withStatus(201)
						.withHeader("Content-Type", "application/json")
						.withBody("""
								{"autorizacaoId":"%s"}
								""".formatted(autorizacaoId))));
	}

	private static WireMockServer iniciarWireMock() {
		WireMockServer server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
		server.start();
		return server;
	}
}

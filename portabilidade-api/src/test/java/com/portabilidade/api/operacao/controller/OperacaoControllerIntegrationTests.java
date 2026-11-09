package com.portabilidade.api.operacao.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.portabilidade.api.autorizacao.entity.Autorizacao;
import com.portabilidade.api.autorizacao.repository.AutorizacaoRepository;
import com.portabilidade.api.operacao.entity.OperacaoPortabilidade;
import com.portabilidade.api.operacao.entity.StatusOperacao;
import com.portabilidade.api.operacao.entity.TipoEventoOperacao;
import com.portabilidade.api.operacao.event.OperacaoEventoService;
import com.portabilidade.api.operacao.repository.OperacaoEventoHistoricoRepository;
import com.portabilidade.api.operacao.repository.OperacaoPortabilidadeRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class OperacaoControllerIntegrationTests {

	private static final WireMockServer wireMock = iniciarWireMock();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AutorizacaoRepository autorizacaoRepository;

	@Autowired
	private OperacaoPortabilidadeRepository operacaoRepository;

	@Autowired
	private OperacaoEventoHistoricoRepository eventoRepository;

	@Autowired
	private OperacaoEventoService eventoService;

	@DynamicPropertySource
	static void centralProperties(DynamicPropertyRegistry registry) {
		registry.add("central.base-url", wireMock::baseUrl);
		registry.add("central.connect-timeout", () -> "2s");
		registry.add("central.read-timeout", () -> "250ms");
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
		eventoRepository.deleteAll();
		operacaoRepository.deleteAll();
		autorizacaoRepository.deleteAll();
	}

	@AfterEach
	void removerDadosDoTeste() {
		eventoRepository.deleteAll();
		operacaoRepository.deleteAll();
		autorizacaoRepository.deleteAll();
	}

	@Test
	void deveConsultarBEN001Com204ERetornarConfirmadaNoProximoPoll() throws Exception {
		OperacaoPortabilidade operacao = criarOperacao("BEN001", "protocolo-ben001", StatusOperacao.PENDENTE);
		stub204Depois200("protocolo-ben001", "BEN001_STATUS", "CONFIRMADA", null);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/portabilidade/operacao/{id}", operacao.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is("PENDENTE")))
				.andExpect(jsonPath("$.retryAfterSeconds", is(2)));

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/portabilidade/operacao/{id}", operacao.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is("CONFIRMADA")))
				.andExpect(jsonPath("$.retryAfterSeconds").doesNotExist())
				.andExpect(jsonPath("$.eventos.length()", is(1)))
				.andExpect(jsonPath("$.eventos[0].tipoEvento", is("OPERACAO_CONFIRMADA")))
				.andExpect(jsonPath("$.eventos[0].statusAnterior", is("PENDENTE")))
				.andExpect(jsonPath("$.eventos[0].statusNovo", is("CONFIRMADA")));

		assertEquals(StatusOperacao.CONFIRMADA,
				operacaoRepository.findById(operacao.getId()).orElseThrow().getStatus());
		wireMock.verify(2, getRequestedFor(urlEqualTo("/operacoes-portabilidade/protocolo-ben001")));
	}

	@Test
	void deveConsultarBEN002Com204ERetornarRejeitadaComMotivo() throws Exception {
		OperacaoPortabilidade operacao = criarOperacao("BEN002", "protocolo-ben002", StatusOperacao.PENDENTE);
		String motivo = "Beneficiário atingiu o limite de margem consignável";
		stub204Depois200("protocolo-ben002", "BEN002_STATUS", "REJEITADA", motivo);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/portabilidade/operacao/{id}", operacao.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is("PENDENTE")))
				.andExpect(jsonPath("$.retryAfterSeconds", is(2)));

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/portabilidade/operacao/{id}", operacao.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is("REJEITADA")))
				.andExpect(jsonPath("$.motivoRejeicao", is(motivo)))
				.andExpect(jsonPath("$.eventos.length()", is(1)))
				.andExpect(jsonPath("$.eventos[0].tipoEvento", is("OPERACAO_REJEITADA")))
				.andExpect(jsonPath("$.eventos[0].statusAnterior", is("PENDENTE")))
				.andExpect(jsonPath("$.eventos[0].statusNovo", is("REJEITADA")));

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/portabilidade/operacao/{id}", operacao.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is("REJEITADA")))
				.andExpect(jsonPath("$.motivoRejeicao", is(motivo)));

		assertEquals(StatusOperacao.REJEITADA,
				operacaoRepository.findById(operacao.getId()).orElseThrow().getStatus());
		wireMock.verify(2, getRequestedFor(urlEqualTo("/operacoes-portabilidade/protocolo-ben002")));
	}

	@Test
	void deveRetornar404ParaOperacaoInexistenteSemChamarCentral() throws Exception {
		UUID operacaoId = UUID.randomUUID();
		mockMvc.perform(MockMvcRequestBuilders.get(
						"/api/v1/portabilidade/operacao/{id}", operacaoId)
						.header("X-Correlation-Id", "corr-operacao-nao-encontrada"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title", is("Operação não encontrada")))
				.andExpect(jsonPath("$.status", is(404)))
				.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString(operacaoId.toString())))
				.andExpect(jsonPath("$.timestamp").isNotEmpty())
				.andExpect(jsonPath("$.operacaoId", is(operacaoId.toString())))
				.andExpect(jsonPath("$.correlationId", is("corr-operacao-nao-encontrada")));

		wireMock.verify(0, getRequestedFor(urlEqualTo("/operacoes-portabilidade/protocolo-inexistente")));
	}

	@Test
	void deveRetornarEstadoPersistidoParaFalhaDeIntegracaoSemProtocolo() throws Exception {
		OperacaoPortabilidade operacao = criarOperacao("BEN_ERRO", null, StatusOperacao.FALHA_INTEGRACAO);
		eventoService.registrarFalhaIntegracao(operacao, "Falha na comunicação com a Central.");

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/portabilidade/operacao/{id}", operacao.getId()))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id", is(operacao.getId().toString())))
				.andExpect(jsonPath("$.status", is("FALHA_INTEGRACAO")))
				.andExpect(jsonPath("$.protocoloCentral").doesNotExist())
				.andExpect(jsonPath("$.eventos[0].tipoEvento", is("OPERACAO_FALHA_INTEGRACAO")));

		wireMock.verify(0, getRequestedFor(anyUrl()));
	}

	@Test
	void naoConsultaCentralParaOperacaoTerminal() throws Exception {
		OperacaoPortabilidade operacao = criarOperacao("BEN001", "protocolo-terminal", StatusOperacao.CONFIRMADA);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/portabilidade/operacao/{id}", operacao.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is("CONFIRMADA")))
				.andExpect(jsonPath("$.retryAfterSeconds").doesNotExist());

		wireMock.verify(0, getRequestedFor(urlEqualTo("/operacoes-portabilidade/protocolo-terminal")));
	}

	@Test
	void deveRetornar404QuandoCentralNaoReconheceProtocolo() throws Exception {
		OperacaoPortabilidade operacao = criarOperacao("BEN001", "protocolo-desconhecido", StatusOperacao.PENDENTE);
		wireMock.stubFor(get(urlEqualTo("/operacoes-portabilidade/protocolo-desconhecido"))
				.willReturn(aResponse()
						.withStatus(404)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"erro\":\"Protocolo não encontrado\"}")));

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/portabilidade/operacao/{id}", operacao.getId()))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void deveRetornar503QuandoCentralIndisponivel() throws Exception {
		OperacaoPortabilidade operacao = criarOperacao("BEN_ERRO", "protocolo-indisponivel", StatusOperacao.PENDENTE);
		wireMock.stubFor(get(urlEqualTo("/operacoes-portabilidade/protocolo-indisponivel"))
				.willReturn(aResponse()
						.withStatus(503)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"erro\":\"Central indisponível\"}")));

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/portabilidade/operacao/{id}", operacao.getId()))
				.andExpect(status().isServiceUnavailable())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

		assertEquals(StatusOperacao.PENDENTE,
				operacaoRepository.findById(operacao.getId()).orElseThrow().getStatus());
		assertEquals(List.of(TipoEventoOperacao.OPERACAO_FALHA_INTEGRACAO),
				eventoRepository.findAllByOperacao_IdOrderByRegistradoEmAscIdAsc(operacao.getId()).stream()
						.map(evento -> evento.getTipoEvento())
						.toList());
	}

	@Test
	void deveRetornarEventosNaOperacaoEmOrdemCronologica() throws Exception {
		OperacaoPortabilidade operacao = criarOperacao("BEN001", "protocolo-timeline", StatusOperacao.PENDENTE);
		eventoService.registrarCriacao(operacao);
		eventoService.registrarProtocolo(operacao);
		wireMock.stubFor(get(urlEqualTo("/operacoes-portabilidade/protocolo-timeline"))
				.willReturn(aResponse().withStatus(204).withHeader("Retry-After", "2")));

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/portabilidade/operacao/{id}", operacao.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.eventos.length()", is(2)))
				.andExpect(jsonPath("$.eventos[0].tipoEvento", is("OPERACAO_CRIADA")))
				.andExpect(jsonPath("$.eventos[0].statusAnterior").doesNotExist())
				.andExpect(jsonPath("$.eventos[0].statusNovo", is("PENDENTE")))
				.andExpect(jsonPath("$.eventos[1].tipoEvento", is("PROTOCOLO_RECEBIDO")))
				.andExpect(jsonPath("$.eventos[1].statusNovo", is("PENDENTE")))
				.andExpect(jsonPath("$.eventos[0].registradoEm").exists())
				.andExpect(jsonPath("$.eventos[1].registradoEm").exists());
	}

	@Test
	void deveRetornar502QuandoRetryAfterForInvalido() throws Exception {
		OperacaoPortabilidade operacao = criarOperacao("BEN001", "protocolo-retry-invalido", StatusOperacao.PENDENTE);
		wireMock.stubFor(get(urlEqualTo("/operacoes-portabilidade/protocolo-retry-invalido"))
				.willReturn(aResponse().withStatus(204).withHeader("Retry-After", "agora")));

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/portabilidade/operacao/{id}", operacao.getId()))
				.andExpect(status().isBadGateway())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title", is("Falha na comunicação com a Central")))
				.andExpect(jsonPath("$.status", is(502)))
				.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Retry-After")))
				.andExpect(jsonPath("$.timestamp").isNotEmpty())
				.andExpect(jsonPath("$.operacaoId", is(operacao.getId().toString())));
	}

	@Test
	void deveRetornar503QuandoConsultaCentralExpira() throws Exception {
		OperacaoPortabilidade operacao = criarOperacao("BEN001", "protocolo-timeout", StatusOperacao.PENDENTE);
		wireMock.stubFor(get(urlEqualTo("/operacoes-portabilidade/protocolo-timeout"))
				.willReturn(aResponse()
						.withFixedDelay(3000)
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"status\":\"CONFIRMADA\",\"motivoRejeicao\":null}")));

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/portabilidade/operacao/{id}", operacao.getId()))
				.andExpect(status().isServiceUnavailable());

		wireMock.verify(3, getRequestedFor(urlEqualTo("/operacoes-portabilidade/protocolo-timeout")));
		assertEquals(StatusOperacao.PENDENTE,
				operacaoRepository.findById(operacao.getId()).orElseThrow().getStatus());
	}

	@Test
	void doisPollsSimultaneosNaoSobrescrevemEstado() throws Exception {
		OperacaoPortabilidade operacao = criarOperacao("BEN001", "protocolo-concorrente", StatusOperacao.PENDENTE);
		wireMock.stubFor(get(urlEqualTo("/operacoes-portabilidade/protocolo-concorrente"))
				.willReturn(aResponse()
						.withFixedDelay(75)
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"status\":\"CONFIRMADA\",\"motivoRejeicao\":null}")));

		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch prontas = new CountDownLatch(2);
		CountDownLatch iniciar = new CountDownLatch(1);
		try {
			Future<MvcResult> primeira = executor.submit(() -> consultarQuandoLiberado(operacao.getId(), prontas, iniciar));
			Future<MvcResult> segunda = executor.submit(() -> consultarQuandoLiberado(operacao.getId(), prontas, iniciar));
			assertTrue(prontas.await(5, TimeUnit.SECONDS));
			iniciar.countDown();

			assertEquals(200, primeira.get(30, TimeUnit.SECONDS).getResponse().getStatus());
			assertEquals(200, segunda.get(30, TimeUnit.SECONDS).getResponse().getStatus());
			OperacaoPortabilidade persistida = operacaoRepository.findById(operacao.getId()).orElseThrow();
			assertEquals(StatusOperacao.CONFIRMADA, persistida.getStatus());
			assertEquals(1L, persistida.getVersion());
			wireMock.verify(2, getRequestedFor(urlEqualTo("/operacoes-portabilidade/protocolo-concorrente")));
		} finally {
			executor.shutdownNow();
		}
	}

	private MvcResult consultarQuandoLiberado(
			UUID operacaoId,
			CountDownLatch prontas,
			CountDownLatch iniciar) throws Exception {
		prontas.countDown();
		if (!iniciar.await(5, TimeUnit.SECONDS)) {
			throw new IllegalStateException("A barreira concorrente não foi liberada.");
		}
		return mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/portabilidade/operacao/{id}", operacaoId))
				.andReturn();
	}

	private OperacaoPortabilidade criarOperacao(String beneficiarioId, String protocolo, StatusOperacao status) {
		Autorizacao autorizacao = autorizacaoRepository.saveAndFlush(
				new Autorizacao(beneficiarioId, "auth-" + beneficiarioId));
		OperacaoPortabilidade operacao = new OperacaoPortabilidade(
				autorizacao,
				"key-" + UUID.randomUUID(),
				"0".repeat(64),
				new BigDecimal("300.00"),
				"conta-destino",
				status);
		operacao.setProtocoloCentral(protocolo);
		return operacaoRepository.saveAndFlush(operacao);
	}

	private void stub204Depois200(String protocolo, String scenario, String status, String motivo) {
		String estadoFinal = "FINAL_" + scenario;
		String motivoJson = motivo == null ? "null" : "\"" + motivo + "\"";
		wireMock.stubFor(get(urlEqualTo("/operacoes-portabilidade/" + protocolo))
				.inScenario(scenario)
				.whenScenarioStateIs(Scenario.STARTED)
				.willReturn(aResponse().withStatus(204).withHeader("Retry-After", "2"))
				.willSetStateTo(estadoFinal));
		wireMock.stubFor(get(urlEqualTo("/operacoes-portabilidade/" + protocolo))
				.inScenario(scenario)
				.whenScenarioStateIs(estadoFinal)
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"status\":\"" + status + "\",\"motivoRejeicao\":" + motivoJson + "}")));
	}

	private static WireMockServer iniciarWireMock() {
		WireMockServer server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
		server.start();
		return server;
	}
}

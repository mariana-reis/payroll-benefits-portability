package com.portabilidade.api.operacao.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.portabilidade.api.autorizacao.entity.Autorizacao;
import com.portabilidade.api.autorizacao.repository.AutorizacaoRepository;
import com.portabilidade.api.operacao.entity.OperacaoPortabilidade;
import com.portabilidade.api.operacao.entity.StatusOperacao;
import com.portabilidade.api.operacao.entity.TipoEventoOperacao;
import com.portabilidade.api.operacao.repository.OperacaoEventoHistoricoRepository;
import com.portabilidade.api.operacao.repository.OperacaoParcelaRepository;
import com.portabilidade.api.operacao.repository.OperacaoPortabilidadeRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class SolicitacaoControllerIntegrationTests {

	private static final String CENTRAL_OPERACOES = "/operacoes-portabilidade";
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
	private OperacaoParcelaRepository operacaoParcelaRepository;

	private static final Pattern OPERACAO_ID = Pattern.compile("\"operacaoId\"\\s*:\\s*\"([^\"]+)\"");

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
	void deveCriarOperacaoBEN001ERepersistirProtocoloParcelasETotal(CapturedOutput output) throws Exception {
		autorizar("BEN001");
		stubAgendaBen001();
		stubOperacao("protocolo-ben001", 202);

		mockMvc.perform(submissao("key-ben001", "BEN001", "[\"P001\",\"P002\"]", "conta-1")
						.header("X-Correlation-Id", "corr-operacao-ben001"))
				.andExpect(status().isAccepted())
				.andExpect(header().string("X-Correlation-Id", "corr-operacao-ben001"))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.protocolo", is("protocolo-ben001")))
				.andExpect(jsonPath("$.status", is("PENDENTE")))
				.andExpect(jsonPath("$.valorTotal", is(300.0)));

		OperacaoPortabilidade operacao = operacaoRepository.findByIdempotencyKey("key-ben001").orElseThrow();
		assertEquals(StatusOperacao.PENDENTE, operacao.getStatus());
		assertEquals("protocolo-ben001", operacao.getProtocoloCentral());
		assertEquals(0, operacao.getValorTotal().compareTo(new java.math.BigDecimal("300.00")));
		assertEquals(2, operacaoParcelaRepository.findAllByOperacao_IdOrderByParcelaIdAsc(operacao.getId()).size());
		assertEquals(List.of(TipoEventoOperacao.OPERACAO_CRIADA, TipoEventoOperacao.PROTOCOLO_RECEBIDO),
				tiposEventos(operacao.getId()));
		assertTrue(eventoRepository.findAllByOperacao_IdOrderByRegistradoEmAscIdAsc(operacao.getId()).stream()
				.allMatch(evento -> "corr-operacao-ben001".equals(evento.getCorrelationId())));
		assertTrue(output.getAll().contains("correlationId=corr-operacao-ben001"));
		assertTrue(output.getAll().contains("operacaoId=" + operacao.getId()));
		assertTrue(output.getAll().contains("status=PENDENTE"));
		wireMock.verify(1, postRequestedFor(urlEqualTo(CENTRAL_OPERACOES))
				.withRequestBody(equalToJson("{\"beneficiarioId\":\"BEN001\"}", true, true))
				.withHeader("X-Correlation-Id", equalTo("corr-operacao-ben001")));
		wireMock.verify(1, getRequestedFor(urlEqualTo("/beneficiarios/BEN001/agenda"))
				.withHeader("X-Correlation-Id", equalTo("corr-operacao-ben001")));
	}

	@Test
	void deveCriarOperacaoBEN002ComUmaParcela() throws Exception {
		autorizar("BEN002");
		stubAgendaBen002();
		stubOperacao("protocolo-ben002", 202);

		mockMvc.perform(submissao("key-ben002", "BEN002", "[\"P010\"]", "conta-2"))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.valorTotal", is(300.0)))
				.andExpect(jsonPath("$.protocolo", is("protocolo-ben002")));

		OperacaoPortabilidade operacao = operacaoRepository.findByIdempotencyKey("key-ben002").orElseThrow();
		assertEquals(1, operacaoParcelaRepository.findAllByOperacao_IdOrderByParcelaIdAsc(operacao.getId()).size());
	}

	@Test
	void deveRetornarMesmaOperacaoParaMesmaChaveEPayload() throws Exception {
		autorizar("BEN001");
		stubAgendaBen001();
		stubOperacao("protocolo-reuso", 202);

		MvcResult primeira = mockMvc.perform(submissao("key-reuso", "BEN001", "[\"P001\",\"P002\"]", "conta-1"))
				.andExpect(status().isAccepted())
				.andReturn();
		MvcResult segunda = mockMvc.perform(submissao("key-reuso", "BEN001", "[\"P002\",\"P001\"]", "conta-1"))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.protocolo", is("protocolo-reuso")))
				.andReturn();

		assertEquals(idOperacao(primeira), idOperacao(segunda));
		assertEquals(1, operacaoRepository.count());
		assertEquals(List.of(
						TipoEventoOperacao.OPERACAO_CRIADA,
						TipoEventoOperacao.PROTOCOLO_RECEBIDO,
						TipoEventoOperacao.OPERACAO_REPROCESSADA),
				tiposEventos(operacaoRepository.findByIdempotencyKey("key-reuso").orElseThrow().getId()));
		wireMock.verify(1, postRequestedFor(urlEqualTo(CENTRAL_OPERACOES)));
		wireMock.verify(1, getRequestedFor(urlEqualTo("/beneficiarios/BEN001/agenda")));
	}

	@Test
	void deveRetornar422QuandoChaveForUsadaComPayloadDiferente() throws Exception {
		autorizar("BEN001");
		stubAgendaBen001();
		stubOperacao("protocolo-conflito", 202);
		mockMvc.perform(submissao("key-conflito", "BEN001", "[\"P001\"]", "conta-1"))
				.andExpect(status().isAccepted());

		mockMvc.perform(submissao("key-conflito", "BEN001", "[\"P002\"]", "conta-1")
						.header("X-Correlation-Id", "corr-idempotencia"))
				.andExpect(status().is(422))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title", is("Solicitação inválida")))
				.andExpect(jsonPath("$.status", is(422)))
				.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("payload diferente")))
				.andExpect(jsonPath("$.timestamp").isNotEmpty())
				.andExpect(jsonPath("$.correlationId", is("corr-idempotencia")));

		assertEquals(1, operacaoRepository.count());
		wireMock.verify(1, postRequestedFor(urlEqualTo(CENTRAL_OPERACOES)));
	}

	@Test
	void deveRetornar400ComProblemDetailParaJsonInvalido() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/portabilidade/solicitar")
						.header("X-Idempotency-Key", "key-json-invalido")
						.header("X-Correlation-Id", "corr-json-invalido")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"beneficiarioId\":"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title", is("JSON inválido")))
				.andExpect(jsonPath("$.status", is(400)))
				.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("JSON válido")))
				.andExpect(jsonPath("$.timestamp").isNotEmpty())
				.andExpect(jsonPath("$.correlationId", is("corr-json-invalido")));
	}

	@Test
	void deveRetornar400ComProblemDetailParaFalhaDeBeanValidation() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/portabilidade/solicitar")
						.header("X-Idempotency-Key", "key-dto-invalido")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "beneficiarioId":"",
								  "parcelaIds":["P001"],
								  "contaDestino":"conta-1"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title", is("Requisição inválida")))
				.andExpect(jsonPath("$.status", is(400)))
				.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("beneficiarioId")))
				.andExpect(jsonPath("$.timestamp").isNotEmpty());
	}

	@Test
	void deveRetornar422ParaParcelaInexistente() throws Exception {
		autorizar("BEN001");
		stubAgendaBen001();

		mockMvc.perform(submissao("key-inexistente", "BEN001", "[\"P999\"]", "conta-1"))
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.detail").value(
						org.hamcrest.Matchers.containsString("não existe na agenda atual")));

		assertEquals(0, operacaoRepository.count());
		wireMock.verify(0, postRequestedFor(urlEqualTo(CENTRAL_OPERACOES)));
	}

	@Test
	void deveRetornar422ParaParcelaFechada() throws Exception {
		autorizar("BEN001");
		wireMock.stubFor(get(urlEqualTo("/beneficiarios/BEN001/agenda"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("""
								[{"parcelaId":"P001","convenioId":"CONV01","valor":150.0,"dataDesconto":"2026-01-10","status":"FECHADA"}]
								""")));

		mockMvc.perform(submissao("key-fechada", "BEN001", "[\"P001\"]", "conta-1"))
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.detail").value(
						org.hamcrest.Matchers.containsString("não está aberta")));

		assertEquals(0, operacaoRepository.count());
	}

	@Test
	void deveRejeitarBeneficiarioSemAgendaValida() throws Exception {
		autorizar("BEN_DESCONHECIDO");
		wireMock.stubFor(get(urlEqualTo("/beneficiarios/BEN_DESCONHECIDO/agenda"))
				.willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("[]")));

		mockMvc.perform(submissao(
						"key-beneficiario-invalido",
						"BEN_DESCONHECIDO",
						"[\"P001\"]",
						"conta-1"))
				.andExpect(status().is(422));

		assertEquals(0, operacaoRepository.count());
		wireMock.verify(0, postRequestedFor(urlEqualTo(CENTRAL_OPERACOES)));
	}

	@Test
	void deveRetornar409SemAutorizacao() throws Exception {
		mockMvc.perform(submissao("key-sem-autorizacao", "BEN001", "[\"P001\"]", "conta-1"))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

		assertEquals(0, operacaoRepository.count());
		wireMock.verify(0, getRequestedFor(urlEqualTo("/beneficiarios/BEN001/agenda")));
		wireMock.verify(0, postRequestedFor(urlEqualTo(CENTRAL_OPERACOES)));
	}

	@Test
	void devePersistirOperacaoPendenteAntesDeRetornarErroDaCentral() throws Exception {
		autorizar("BEN_ERRO");
		wireMock.stubFor(get(urlEqualTo("/beneficiarios/BEN_ERRO/agenda"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("""
								[{"parcelaId":"P001","convenioId":"CONV01","valor":150.0,"dataDesconto":"2026-01-10","status":"ABERTA"}]
								""")));
		wireMock.stubFor(post(urlEqualTo(CENTRAL_OPERACOES))
				.inScenario("BEN_ERRO-reprocessamento")
				.whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
				.willReturn(aResponse()
						.withStatus(503)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"erro\":\"Central indisponível\"}"))
				.willSetStateTo("central-disponivel"));
		wireMock.stubFor(post(urlEqualTo(CENTRAL_OPERACOES))
				.inScenario("BEN_ERRO-reprocessamento")
				.whenScenarioStateIs("central-disponivel")
				.willReturn(aResponse()
						.withStatus(202)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"protocolo\":\"protocolo-reprocessado\"}")));

		mockMvc.perform(submissao("key-central-erro", "BEN_ERRO", "[\"P001\"]", "conta-1"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.operacaoId").value(
						operacaoRepository.findByIdempotencyKey("key-central-erro").orElseThrow()
								.getId().toString()));
		wireMock.verify(1, postRequestedFor(urlEqualTo(CENTRAL_OPERACOES)));

		OperacaoPortabilidade persistida = operacaoRepository.findByIdempotencyKey("key-central-erro").orElseThrow();
		assertEquals(StatusOperacao.FALHA_INTEGRACAO, persistida.getStatus());
		assertNull(persistida.getProtocoloCentral());
		assertEquals(1, operacaoParcelaRepository.findAllByOperacao_IdOrderByParcelaIdAsc(persistida.getId()).size());
		var eventosAposFalha = eventoRepository.findAllByOperacao_IdOrderByRegistradoEmAscIdAsc(persistida.getId());
		assertEquals(List.of(
						TipoEventoOperacao.OPERACAO_CRIADA,
						TipoEventoOperacao.OPERACAO_FALHA_INTEGRACAO),
				eventosAposFalha.stream().map(evento -> evento.getTipoEvento()).toList());
		assertEquals(StatusOperacao.PENDENTE, eventosAposFalha.get(1).getStatusAnterior());
		assertEquals(StatusOperacao.FALHA_INTEGRACAO, eventosAposFalha.get(1).getStatusNovo());

		mockMvc.perform(submissao("key-central-erro", "BEN_ERRO", "[\"P001\"]", "conta-1"))
				.andExpect(status().isAccepted())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
						"$.status", is("PENDENTE")))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
						"$.protocolo", is("protocolo-reprocessado")));
		OperacaoPortabilidade reprocessada = operacaoRepository.findById(persistida.getId()).orElseThrow();
		assertEquals(1, operacaoRepository.count());
		assertEquals(StatusOperacao.PENDENTE, reprocessada.getStatus());
		assertEquals("protocolo-reprocessado", reprocessada.getProtocoloCentral());
		var eventosAposRetentativa =
				eventoRepository.findAllByOperacao_IdOrderByRegistradoEmAscIdAsc(persistida.getId());
		assertEquals(List.of(
						TipoEventoOperacao.OPERACAO_CRIADA,
						TipoEventoOperacao.OPERACAO_FALHA_INTEGRACAO,
						TipoEventoOperacao.OPERACAO_REPROCESSADA,
						TipoEventoOperacao.PROTOCOLO_RECEBIDO),
				eventosAposRetentativa.stream().map(evento -> evento.getTipoEvento()).toList());
		assertEquals(StatusOperacao.FALHA_INTEGRACAO, eventosAposRetentativa.get(2).getStatusAnterior());
		assertEquals(StatusOperacao.PENDENTE, eventosAposRetentativa.get(2).getStatusNovo());
		wireMock.verify(2, postRequestedFor(urlEqualTo(CENTRAL_OPERACOES)));
		wireMock.verify(1, getRequestedFor(urlEqualTo("/beneficiarios/BEN_ERRO/agenda")));
	}

	@Test
	void deveIncluirOperacaoIdNoProblemDetailParaFalhaDeIntegracao502() throws Exception {
		autorizar("BEN001");
		stubAgendaBen001();
		wireMock.stubFor(post(urlEqualTo(CENTRAL_OPERACOES))
				.willReturn(aResponse()
						.withStatus(202)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"protocolo\":\"\"}")));

		mockMvc.perform(submissao("key-central-resposta-invalida", "BEN001", "[\"P001\"]", "conta-1"))
				.andExpect(status().isBadGateway())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status", is(502)))
				.andExpect(jsonPath("$.operacaoId").value(
						operacaoRepository.findByIdempotencyKey("key-central-resposta-invalida").orElseThrow()
								.getId().toString()));

		assertEquals(StatusOperacao.FALHA_INTEGRACAO,
				operacaoRepository.findByIdempotencyKey("key-central-resposta-invalida").orElseThrow().getStatus());
		wireMock.verify(1, postRequestedFor(urlEqualTo(CENTRAL_OPERACOES)));
	}

	@Test
	void duasThreadsComMesmaChaveEPayloadCriamUmaOperacao() throws Exception {
		autorizar("BEN001");
		stubAgendaBen001();
		stubOperacao("protocolo-concorrente", 202);

		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch prontas = new CountDownLatch(2);
		CountDownLatch iniciar = new CountDownLatch(1);
		try {
			Future<MvcResult> primeira = executor.submit(() -> executarQuandoLiberada(prontas, iniciar));
			Future<MvcResult> segunda = executor.submit(() -> executarQuandoLiberada(prontas, iniciar));

			assertTrue(prontas.await(5, TimeUnit.SECONDS));
			iniciar.countDown();
			MvcResult resultadoPrimeiro = primeira.get(30, TimeUnit.SECONDS);
			MvcResult resultadoSegundo = segunda.get(30, TimeUnit.SECONDS);

			assertEquals(202, resultadoPrimeiro.getResponse().getStatus());
			assertEquals(202, resultadoSegundo.getResponse().getStatus());
			assertEquals(idOperacao(resultadoPrimeiro), idOperacao(resultadoSegundo));
			assertEquals(1, operacaoRepository.count());
			wireMock.verify(1, postRequestedFor(urlEqualTo(CENTRAL_OPERACOES)));
		} finally {
			executor.shutdownNow();
		}
	}

	private MvcResult executarQuandoLiberada(CountDownLatch prontas, CountDownLatch iniciar) throws Exception {
		prontas.countDown();
		if (!iniciar.await(5, TimeUnit.SECONDS)) {
			throw new IllegalStateException("A barreira concorrente não foi liberada.");
		}
		return mockMvc.perform(submissao(
						"key-concorrente",
						"BEN001",
						"[\"P001\",\"P002\"]",
						"conta-1"))
				.andReturn();
	}

	private MockHttpServletRequestBuilder submissao(
			String chave,
			String beneficiarioId,
			String parcelaIds,
			String contaDestino) {
		return MockMvcRequestBuilders.post("/api/v1/portabilidade/solicitar")
				.header("X-Idempotency-Key", chave)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "beneficiarioId":"%s",
						  "parcelaIds":%s,
						  "contaDestino":"%s"
						}
						""".formatted(beneficiarioId, parcelaIds, contaDestino));
	}

	private String idOperacao(MvcResult result) throws Exception {
		Matcher matcher = OPERACAO_ID.matcher(result.getResponse().getContentAsString());
		if (!matcher.find()) {
			throw new AssertionError("A resposta não contém operacaoId.");
		}
		return matcher.group(1);
	}

	private void autorizar(String beneficiarioId) {
		autorizacaoRepository.saveAndFlush(new Autorizacao(beneficiarioId, "auth-" + beneficiarioId));
	}

	private List<TipoEventoOperacao> tiposEventos(java.util.UUID operacaoId) {
		return eventoRepository.findAllByOperacao_IdOrderByRegistradoEmAscIdAsc(operacaoId).stream()
				.map(evento -> evento.getTipoEvento())
				.toList();
	}

	private void stubAgendaBen001() {
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
	}

	private void stubAgendaBen002() {
		wireMock.stubFor(get(urlEqualTo("/beneficiarios/BEN002/agenda"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("""
								[{"parcelaId":"P010","convenioId":"CONV02","valor":300.0,"dataDesconto":"2026-01-15","status":"ABERTA"}]
								""")));
	}

	private void stubOperacao(String protocolo, int statusCode) {
		String body = protocolo == null ? "{\"erro\":\"Central indisponível\"}"
				: "{\"protocolo\":\"" + protocolo + "\"}";
		wireMock.stubFor(post(urlEqualTo(CENTRAL_OPERACOES))
				.willReturn(aResponse()
						.withStatus(statusCode)
						.withHeader("Content-Type", "application/json")
						.withBody(body)));
	}

	private static WireMockServer iniciarWireMock() {
		WireMockServer server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
		server.start();
		return server;
	}
}

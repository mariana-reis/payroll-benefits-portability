package com.portabilidade.api.central.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.portabilidade.api.central.dto.AgendaResponse;
import com.portabilidade.api.central.dto.AutorizacaoResponse;
import com.portabilidade.api.central.dto.OperacaoPortabilidadeResponse;
import com.portabilidade.api.central.dto.StatusOperacaoResponse;
import com.portabilidade.api.central.exception.CentralIndisponivelException;
import com.portabilidade.api.central.exception.ProtocoloNaoEncontradoException;
import com.portabilidade.api.central.mapper.CentralExceptionMapper;
import com.portabilidade.api.central.properties.CentralProperties;
import com.portabilidade.api.central.properties.CentralRetryProperties;
import com.portabilidade.api.central.security.HmacSigner;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class CentralClientTests {

	private static final WireMockServer wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
	private static final String HMAC_SECRET = "test-secret";
	private final CentralClientConfig config = new CentralClientConfig();
	private final CentralExceptionMapper exceptionMapper = new CentralExceptionMapper();

	@BeforeAll
	static void iniciarWireMock() {
		wireMock.start();
	}

	@AfterAll
	static void pararWireMock() {
		wireMock.stop();
	}

	@BeforeEach
	void limparStubs() {
		wireMock.resetAll();
	}

	@Test
	void registraAutorizacaoComResposta201() {
		wireMock.stubFor(post(urlEqualTo("/autorizacoes"))
				.willReturn(aResponse()
						.withStatus(201)
						.withHeader("Content-Type", "application/json")
						.withBody("""
								{"autorizacaoId":"auth-123"}
								""")));

		AutorizacaoResponse response = client().registrarAutorizacao(Map.of());

		assertEquals("auth-123", response.autorizacaoId());
		assertSignedPost("/autorizacoes", "{}");
	}

	@Test
	void consultaAgendaComResposta200() {
		wireMock.stubFor(get(urlEqualTo("/beneficiarios/BEN001/agenda"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("""
								[{
								  "parcelaId":"P001",
								  "convenioId":"CONV01",
								  "valor":150.0,
								  "dataDesconto":"2026-01-10",
								  "status":"ABERTA"
								}]
								""")));

		AgendaResponse response = client().consultarAgenda("BEN001");

		assertEquals(1, response.parcelas().size());
		assertEquals("P001", response.parcelas().getFirst().parcelaId());
		assertEquals("CONV01", response.parcelas().getFirst().convenioId());
		assertEquals(new BigDecimal("150.0"), response.parcelas().getFirst().valor());
		assertEquals(LocalDate.parse("2026-01-10"), response.parcelas().getFirst().dataDesconto());
		assertEquals("ABERTA", response.parcelas().getFirst().status());
	}

	@Test
	void solicitaOperacaoComResposta202() {
		wireMock.stubFor(post(urlEqualTo("/operacoes-portabilidade"))
				.willReturn(aResponse()
						.withStatus(202)
						.withHeader("Content-Type", "application/json")
						.withBody("""
								{"protocolo":"protocolo-123"}
								""")));

		OperacaoPortabilidadeResponse response = client()
				.solicitarPortabilidade(Map.of("beneficiarioId", "BEN001"));

		assertEquals("protocolo-123", response.protocolo());
		assertSignedPost("/operacoes-portabilidade", "{\"beneficiarioId\":\"BEN001\"}");
	}

	@Test
	void consultaStatusPendenteComRetryAfterEm204() {
		wireMock.stubFor(get(urlEqualTo("/operacoes-portabilidade/protocolo-pendente"))
				.willReturn(aResponse()
						.withStatus(204)
						.withHeader("Retry-After", "2")));

		StatusOperacaoResponse response = client().consultarStatus("protocolo-pendente");

		assertNull(response.status());
		assertNull(response.motivoRejeicao());
		assertEquals(2L, response.retryAfterSeconds());
	}

	@Test
	void consultaStatusConfirmadoComResposta200() {
		wireMock.stubFor(get(urlEqualTo("/operacoes-portabilidade/protocolo-ok"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("""
								{"status":"CONFIRMADA","motivoRejeicao":null}
								""")));

		StatusOperacaoResponse response = client().consultarStatus("protocolo-ok");

		assertEquals("CONFIRMADA", response.status());
		assertNull(response.motivoRejeicao());
		assertNull(response.retryAfterSeconds());
	}

	@Test
	void consultaStatusRejeitadoComMotivo() {
		wireMock.stubFor(get(urlEqualTo("/operacoes-portabilidade/protocolo-rejeitado"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("""
								{
								  "status":"REJEITADA",
								  "motivoRejeicao":"Beneficiário atingiu o limite de margem consignável"
								}
								""")));

		StatusOperacaoResponse response = client().consultarStatus("protocolo-rejeitado");

		assertEquals("REJEITADA", response.status());
		assertEquals("Beneficiário atingiu o limite de margem consignável", response.motivoRejeicao());
	}

	@Test
	void protocoloInexistenteMapeiaParaExcecaoEspecifica() {
		wireMock.stubFor(get(urlEqualTo("/operacoes-portabilidade/inexistente"))
				.willReturn(aResponse()
						.withStatus(404)
						.withHeader("Content-Type", "application/json")
						.withBody("""
								{"erro":"Protocolo não encontrado"}
								""")));

		ProtocoloNaoEncontradoException exception = assertThrows(
				ProtocoloNaoEncontradoException.class,
				() -> client().consultarStatus("inexistente"));

		assertEquals("Protocolo não encontrado em GET /operacoes-portabilidade/{protocolo}: "
				+ "Protocolo não encontrado", exception.getMessage());
	}

	@Test
	void indisponibilidadeDaCentralMapeiaParaExcecao() {
		wireMock.stubFor(post(urlEqualTo("/operacoes-portabilidade"))
				.willReturn(aResponse()
						.withStatus(503)
						.withHeader("Content-Type", "application/json")
						.withBody("""
								{"erro":"Central indisponível no momento (simulado). Tente novamente."}
								""")));

		CentralIndisponivelException exception = assertThrows(
				CentralIndisponivelException.class,
				() -> client().solicitarPortabilidade(Map.of("beneficiarioId", "BEN_ERRO")));

		assertTrue(exception.getMessage().contains("Central indisponível no momento"));
		wireMock.verify(1, postRequestedFor(urlEqualTo("/operacoes-portabilidade")));
	}

	@Test
	void postAutorizacaoNaoEhRepetido() {
		wireMock.stubFor(post(urlEqualTo("/autorizacoes"))
				.willReturn(aResponse()
						.withStatus(503)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"erro\":\"Central indisponível\"}")));

		assertThrows(CentralIndisponivelException.class, () -> client().registrarAutorizacao(Map.of()));

		wireMock.verify(1, postRequestedFor(urlEqualTo("/autorizacoes")));
	}

	@Test
	void timeoutMapeiaParaCentralIndisponivel() {
		wireMock.stubFor(get(urlEqualTo("/beneficiarios/BEN001/agenda"))
				.willReturn(aResponse()
						.withFixedDelay(2000)
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("[]")));

		assertThrows(
				CentralIndisponivelException.class,
				() -> client(Duration.ofMillis(500)).consultarAgenda("BEN001"));
		wireMock.verify(3, getRequestedFor(urlEqualTo("/beneficiarios/BEN001/agenda")));
	}

	@Test
	void conexaoRecusadaMapeiaParaCentralIndisponivel() throws IOException {
		int port;
		try (ServerSocket socket = new ServerSocket(0)) {
			port = socket.getLocalPort();
		}
		CentralClient client = client(URI.create("http://127.0.0.1:" + port), Duration.ofMillis(500));

		assertThrows(CentralIndisponivelException.class, () -> client.consultarAgenda("BEN001"));
	}

	@Test
	void retryAgendaApos503RecuperaNaTentativaSeguinte() {
		wireMock.stubFor(get(urlEqualTo("/beneficiarios/BEN099/agenda"))
				.inScenario("agenda-retry")
				.whenScenarioStateIs(Scenario.STARTED)
				.willReturn(aResponse()
						.withStatus(503)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"erro\":\"temporário\"}"))
				.willSetStateTo("agenda-disponivel"));
		wireMock.stubFor(get(urlEqualTo("/beneficiarios/BEN099/agenda"))
				.inScenario("agenda-retry")
				.whenScenarioStateIs("agenda-disponivel")
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("[]")));

		assertEquals(0, client().consultarAgenda("BEN099").parcelas().size());

		wireMock.verify(2, getRequestedFor(urlEqualTo("/beneficiarios/BEN099/agenda")));
	}

	@Test
	void retryAgendaAposDuasFalhasRecuperaNaTerceiraTentativa() {
		wireMock.stubFor(get(urlEqualTo("/beneficiarios/BEN097/agenda"))
				.inScenario("agenda-duas-falhas")
				.whenScenarioStateIs(Scenario.STARTED)
				.willReturn(aResponse()
						.withStatus(503)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"erro\":\"indisponível\"}"))
				.willSetStateTo("agenda-segunda-falha"));
		wireMock.stubFor(get(urlEqualTo("/beneficiarios/BEN097/agenda"))
				.inScenario("agenda-duas-falhas")
				.whenScenarioStateIs("agenda-segunda-falha")
				.willReturn(aResponse()
						.withStatus(503)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"erro\":\"indisponível\"}"))
				.willSetStateTo("agenda-disponivel"));
		wireMock.stubFor(get(urlEqualTo("/beneficiarios/BEN097/agenda"))
				.inScenario("agenda-duas-falhas")
				.whenScenarioStateIs("agenda-disponivel")
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("[]")));

		assertEquals(0, client().consultarAgenda("BEN097").parcelas().size());

		wireMock.verify(3, getRequestedFor(urlEqualTo("/beneficiarios/BEN097/agenda")));
	}

	@Test
	void falhaPersistenteEPropagadaAposLimiteDeTentativas() {
		wireMock.stubFor(get(urlEqualTo("/beneficiarios/BEN096/agenda"))
				.willReturn(aResponse()
						.withStatus(503)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"erro\":\"indisponível\"}")));

		assertThrows(CentralIndisponivelException.class, () -> client().consultarAgenda("BEN096"));

		wireMock.verify(3, getRequestedFor(urlEqualTo("/beneficiarios/BEN096/agenda")));
	}

	@Test
	void retryStatusApos503RecuperaNaTentativaSeguinte() {
		wireMock.stubFor(get(urlEqualTo("/operacoes-portabilidade/protocolo-retry"))
				.inScenario("status-retry")
				.whenScenarioStateIs(Scenario.STARTED)
				.willReturn(aResponse()
						.withStatus(503)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"erro\":\"temporário\"}"))
				.willSetStateTo("status-disponivel"));
		wireMock.stubFor(get(urlEqualTo("/operacoes-portabilidade/protocolo-retry"))
				.inScenario("status-retry")
				.whenScenarioStateIs("status-disponivel")
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"status\":\"CONFIRMADA\",\"motivoRejeicao\":null}")));

		assertEquals("CONFIRMADA", client().consultarStatus("protocolo-retry").status());

		wireMock.verify(2, getRequestedFor(urlEqualTo("/operacoes-portabilidade/protocolo-retry")));
	}

	@Test
	void erroDefinitivoNaoEhRepetido() {
		wireMock.stubFor(get(urlEqualTo("/operacoes-portabilidade/protocolo-ausente"))
				.willReturn(aResponse()
						.withStatus(404)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"erro\":\"Protocolo não encontrado\"}")));

		assertThrows(
				ProtocoloNaoEncontradoException.class,
				() -> client().consultarStatus("protocolo-ausente"));

		wireMock.verify(1, getRequestedFor(urlEqualTo("/operacoes-portabilidade/protocolo-ausente")));
	}

	@Test
	void timeoutDaAgendaRecuperaNaTentativaSeguinte() {
		wireMock.stubFor(get(urlEqualTo("/beneficiarios/BEN098/agenda"))
				.inScenario("timeout-agenda")
				.whenScenarioStateIs(Scenario.STARTED)
				.willReturn(aResponse()
						.withFixedDelay(500)
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("[]"))
				.willSetStateTo("agenda-sem-timeout"));
		wireMock.stubFor(get(urlEqualTo("/beneficiarios/BEN098/agenda"))
				.inScenario("timeout-agenda")
				.whenScenarioStateIs("agenda-sem-timeout")
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("[]")));

		assertEquals(0, client(Duration.ofMillis(100)).consultarAgenda("BEN098").parcelas().size());

		wireMock.verify(2, getRequestedFor(urlEqualTo("/beneficiarios/BEN098/agenda")));
	}

	private CentralClient client() {
		return client(Duration.ofSeconds(2));
	}

	private CentralClient client(Duration readTimeout) {
		return client(URI.create(wireMock.baseUrl()), readTimeout);
	}

	private CentralClient client(URI baseUrl, Duration readTimeout) {
		CentralProperties properties = new CentralProperties(
				baseUrl,
				Duration.ofSeconds(2),
				readTimeout,
				HMAC_SECRET);
		RestClient restClient = config.centralRestClient(RestClient.builder(), properties);
		return new CentralClient(
				restClient,
				exceptionMapper,
				new ObjectMapper(),
				new HmacSigner(properties.hmacSecret()),
				new CentralGetRetry(new CentralRetryProperties(
						3,
						Duration.ZERO,
						Duration.ZERO,
						2)));
	}

	private void assertSignedPost(String path, String expectedBody) {
		var request = wireMock.getAllServeEvents().stream()
				.map(event -> event.getRequest())
				.filter(logged -> logged.getUrl().equals(path))
				.findFirst()
				.orElseThrow();
		String timestamp = request.getHeader("X-Timestamp");
		String signature = request.getHeader("X-Signature");
		assertTrue(timestamp.matches("\\d+"));
		assertEquals(expectedBody, request.getBodyAsString());
		assertTrue(request.getHeader("Content-Type").startsWith("application/json"));
		assertEquals(calcularAssinatura(timestamp, request.getBody()), signature);
	}

	private String calcularAssinatura(String timestamp, byte[] body) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(HMAC_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			mac.update(timestamp.getBytes(StandardCharsets.UTF_8));
			mac.update((byte) '.');
			return java.util.HexFormat.of().formatHex(mac.doFinal(body));
		} catch (NoSuchAlgorithmException | InvalidKeyException exception) {
			throw new AssertionError("Falha ao recalcular a assinatura no teste.", exception);
		}
	}
}

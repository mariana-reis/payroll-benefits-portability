package com.portabilidade.api.central.client;

import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

import com.portabilidade.api.central.dto.AgendaResponse;
import com.portabilidade.api.central.dto.AutorizacaoResponse;
import com.portabilidade.api.central.dto.OperacaoPortabilidadeResponse;
import com.portabilidade.api.central.dto.ParcelaResponse;
import com.portabilidade.api.central.dto.StatusOperacaoResponse;
import com.portabilidade.api.central.exception.CentralClientException;
import com.portabilidade.api.central.exception.CentralIndisponivelException;
import com.portabilidade.api.central.exception.ProtocoloNaoEncontradoException;
import com.portabilidade.api.central.mapper.CentralExceptionMapper;
import com.portabilidade.api.central.security.HmacSigner;
import com.portabilidade.api.config.observability.CorrelationIdFilter;
import org.slf4j.MDC;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

public class CentralClient {

	private static final ParameterizedTypeReference<List<ParcelaResponse>> AGENDA_TYPE =
			new ParameterizedTypeReference<>() {
			};

	private final RestClient restClient;
	private final CentralExceptionMapper exceptionMapper;
	private final ObjectMapper objectMapper;
	private final HmacSigner hmacSigner;
	private final CentralGetRetry centralGetRetry;

	public CentralClient(
			RestClient restClient,
			CentralExceptionMapper exceptionMapper,
			ObjectMapper objectMapper,
			HmacSigner hmacSigner,
			CentralGetRetry centralGetRetry) {
		this.restClient = restClient;
		this.exceptionMapper = exceptionMapper;
		this.objectMapper = objectMapper;
		this.hmacSigner = hmacSigner;
		this.centralGetRetry = centralGetRetry;
	}

	public AutorizacaoResponse registrarAutorizacao(Object request) {
		return executar("POST /autorizacoes", false, () -> {
			byte[] body = serializar(request);
			String timestamp = Long.toString(Instant.now().getEpochSecond());
			String signature = hmacSigner.sign(timestamp, body);
			var requestSpec = restClient.post();
			adicionarCorrelationId(requestSpec);
			ResponseEntity<AutorizacaoResponse> response = requestSpec
					.uri("/autorizacoes")
					.contentType(MediaType.APPLICATION_JSON)
					.header("X-Timestamp", timestamp)
					.header("X-Signature", signature)
					.body(body)
					.retrieve()
					.toEntity(AutorizacaoResponse.class);
			verificarStatus(response, HttpStatus.CREATED, "POST /autorizacoes");
			return response.getBody();
		});
	}

	public AgendaResponse consultarAgenda(String beneficiarioId) {
		return centralGetRetry.executar(() -> executar("GET /beneficiarios/{id}/agenda", false, () -> {
			var requestSpec = restClient.get();
			adicionarCorrelationId(requestSpec);
			ResponseEntity<List<ParcelaResponse>> response = requestSpec
					.uri("/beneficiarios/{id}/agenda", beneficiarioId)
					.retrieve()
					.toEntity(AGENDA_TYPE);
			verificarStatus(response, HttpStatus.OK, "GET /beneficiarios/{id}/agenda");
			List<ParcelaResponse> parcelas = response.getBody();
			if (parcelas == null) {
				throw new CentralClientException("A Central retornou uma agenda sem corpo.");
			}
			return new AgendaResponse(parcelas);
		}));
	}

	public OperacaoPortabilidadeResponse solicitarPortabilidade(Object request) {
		return executar("POST /operacoes-portabilidade", false, () -> {
			byte[] body = serializar(request);
			String timestamp = Long.toString(Instant.now().getEpochSecond());
			String signature = hmacSigner.sign(timestamp, body);
			var requestSpec = restClient.post();
			adicionarCorrelationId(requestSpec);
			ResponseEntity<OperacaoPortabilidadeResponse> response = requestSpec
					.uri("/operacoes-portabilidade")
					.contentType(MediaType.APPLICATION_JSON)
					.header("X-Timestamp", timestamp)
					.header("X-Signature", signature)
					.body(body)
					.retrieve()
					.toEntity(OperacaoPortabilidadeResponse.class);
			verificarStatus(response, HttpStatus.ACCEPTED, "POST /operacoes-portabilidade");
			return response.getBody();
		});
	}

	private byte[] serializar(Object request) {
		try {
			return objectMapper.writeValueAsBytes(request);
		} catch (JacksonException exception) {
			throw new CentralClientException("Falha ao serializar a requisição para a Central.", exception);
		}
	}

	public StatusOperacaoResponse consultarStatus(String protocolo) {
		return centralGetRetry.executar(() -> executar("GET /operacoes-portabilidade/{protocolo}", true, () -> {
			var requestSpec = restClient.get();
			adicionarCorrelationId(requestSpec);
			ResponseEntity<StatusOperacaoResponse> response = requestSpec
					.uri("/operacoes-portabilidade/{protocolo}", protocolo)
					.retrieve()
					.toEntity(StatusOperacaoResponse.class);

			if (response.getStatusCode().value() == 204) {
				return new StatusOperacaoResponse(null, null, lerRetryAfter(response));
			}
			verificarStatus(response, HttpStatus.OK, "GET /operacoes-portabilidade/{protocolo}");

			StatusOperacaoResponse body = response.getBody();
			if (body == null) {
				throw new CentralClientException("A Central retornou status sem corpo.");
			}
			return new StatusOperacaoResponse(body.status(), body.motivoRejeicao(), null);
		}));
	}

	private void adicionarCorrelationId(RestClient.RequestHeadersSpec<?> requestSpec) {
		String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
		if (correlationId != null) {
			requestSpec.header(CorrelationIdFilter.HEADER_NAME, correlationId);
		}
	}

	private <T> T executar(String chamada, boolean protocoloNaoEncontrado, Supplier<T> request) {
		try {
			T response = request.get();
			if (response == null) {
				throw new CentralClientException("A Central retornou resposta vazia em " + chamada + ".");
			}
			return response;
		} catch (CentralClientException | CentralIndisponivelException
				| ProtocoloNaoEncontradoException exception) {
			throw exception;
		} catch (RestClientResponseException exception) {
			throw exceptionMapper.map(exception, chamada, protocoloNaoEncontrado);
		} catch (ResourceAccessException exception) {
			throw new CentralIndisponivelException("Falha de conexão ou timeout ao chamar a Central em "
					+ chamada + ".", exception);
		} catch (RestClientException exception) {
			throw new CentralClientException("Falha ao processar a resposta da Central em " + chamada + ".", exception);
		}
	}

	private void verificarStatus(
			ResponseEntity<?> response,
			HttpStatusCode esperado,
			String chamada) {
		if (!response.getStatusCode().equals(esperado)) {
			throw new CentralClientException(
					"A Central respondeu HTTP " + response.getStatusCode().value()
							+ " em " + chamada + "; esperado HTTP " + esperado.value() + ".");
		}
	}

	private Long lerRetryAfter(ResponseEntity<?> response) {
		String retryAfter = response.getHeaders().getFirst("Retry-After");
		if (retryAfter == null) {
			return null;
		}
		try {
			return Long.valueOf(retryAfter);
		} catch (NumberFormatException exception) {
			throw new CentralClientException("A Central retornou um cabeçalho Retry-After inválido.", exception);
		}
	}
}

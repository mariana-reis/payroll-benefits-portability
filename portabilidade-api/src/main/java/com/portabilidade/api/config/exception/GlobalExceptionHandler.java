package com.portabilidade.api.config.exception;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.portabilidade.api.agenda.exception.AutorizacaoObrigatoriaException;
import com.portabilidade.api.central.exception.CentralClientException;
import com.portabilidade.api.central.exception.CentralIndisponivelException;
import com.portabilidade.api.central.exception.ProtocoloNaoEncontradoException;
import com.portabilidade.api.operacao.exception.IdempotencyPayloadMismatchException;
import com.portabilidade.api.operacao.exception.CentralClientOperacaoException;
import com.portabilidade.api.operacao.exception.CentralIndisponivelOperacaoException;
import com.portabilidade.api.operacao.exception.OperacaoNaoEncontradaException;
import com.portabilidade.api.operacao.exception.SolicitacaoInvalidaException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

	@ExceptionHandler(CentralIndisponivelException.class)
	public ResponseEntity<ProblemDetail> centralIndisponivel(
			CentralIndisponivelException exception,
			WebRequest request) {
		return problema(HttpStatus.SERVICE_UNAVAILABLE, "Central indisponível", exception.getMessage(), request);
	}

	@ExceptionHandler(CentralClientException.class)
	public ResponseEntity<ProblemDetail> falhaIntegracao(CentralClientException exception, WebRequest request) {
		return problema(HttpStatus.BAD_GATEWAY, "Falha na comunicação com a Central", exception.getMessage(), request);
	}

	@ExceptionHandler(CentralIndisponivelOperacaoException.class)
	public ResponseEntity<ProblemDetail> falhaIntegracaoOperacaoIndisponivel(
			CentralIndisponivelOperacaoException exception,
			WebRequest request) {
		return problemaIntegracaoOperacao(
				HttpStatus.SERVICE_UNAVAILABLE,
				"Central indisponível",
				exception.getMessage(),
				exception.getOperacaoId(),
				request);
	}

	@ExceptionHandler(CentralClientOperacaoException.class)
	public ResponseEntity<ProblemDetail> falhaIntegracaoOperacaoCliente(
			CentralClientOperacaoException exception,
			WebRequest request) {
		return problemaIntegracaoOperacao(
				HttpStatus.BAD_GATEWAY,
				"Falha na comunicação com a Central",
				exception.getMessage(),
				exception.getOperacaoId(),
				request);
	}

	private ResponseEntity<ProblemDetail> problemaIntegracaoOperacao(
			HttpStatus status,
			String title,
			String detail,
			UUID operacaoId,
			WebRequest request) {
		ProblemDetail problem = criarProblema(
				status,
				title,
				detail);
		problem.setProperty("operacaoId", operacaoId);
		adicionarMetadados(problem, request);
		return ResponseEntity.status(status)
				.contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.body(problem);
	}

	@ExceptionHandler(ProtocoloNaoEncontradoException.class)
	public ResponseEntity<ProblemDetail> protocoloNaoEncontrado(
			ProtocoloNaoEncontradoException exception,
			WebRequest request) {
		return problema(HttpStatus.NOT_FOUND, "Protocolo não encontrado", exception.getMessage(), request);
	}

	@ExceptionHandler(OperacaoNaoEncontradaException.class)
	public ResponseEntity<ProblemDetail> operacaoNaoEncontrada(
			OperacaoNaoEncontradaException exception,
			WebRequest request) {
		ProblemDetail problem = criarProblema(HttpStatus.NOT_FOUND, "Operação não encontrada",
				exception.getMessage());
		problem.setProperty("operacaoId", exception.getOperacaoId());
		adicionarMetadados(problem, request);
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.body(problem);
	}

	@ExceptionHandler(AutorizacaoObrigatoriaException.class)
	public ResponseEntity<ProblemDetail> autorizacaoObrigatoria(
			AutorizacaoObrigatoriaException exception,
			WebRequest request) {
		return problema(HttpStatus.CONFLICT, "Autorização obrigatória", exception.getMessage(), request);
	}

	@ExceptionHandler({ IdempotencyPayloadMismatchException.class, SolicitacaoInvalidaException.class })
	public ResponseEntity<ProblemDetail> solicitacaoInvalida(RuntimeException exception, WebRequest request) {
		return problema(HttpStatusCode.valueOf(422), "Solicitação inválida", exception.getMessage(), request);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ProblemDetail> violacaoDeConstraint(
			ConstraintViolationException exception,
			WebRequest request) {
		return problema(HttpStatus.BAD_REQUEST, "Requisição inválida", exception.getMessage(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ProblemDetail> argumentoInvalido(
			MethodArgumentNotValidException exception,
			WebRequest request) {
		String detail = exception.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(this::detalheValidacao)
				.orElse("Um ou mais campos são inválidos.");
		return problema(HttpStatus.BAD_REQUEST, "Requisição inválida", detail, request);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ProblemDetail> jsonInvalido(
			HttpMessageNotReadableException exception,
			WebRequest request) {
		return problema(HttpStatus.BAD_REQUEST, "JSON inválido",
				"O corpo da requisição não é um JSON válido.", request);
	}

	@ExceptionHandler({ MethodArgumentTypeMismatchException.class, MissingRequestHeaderException.class })
	public ResponseEntity<ProblemDetail> argumentoHttpInvalido(Exception exception, WebRequest request) {
		return problema(HttpStatus.BAD_REQUEST, "Requisição inválida", exception.getMessage(), request);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ProblemDetail> metodoNaoSuportado(
			HttpRequestMethodNotSupportedException exception,
			WebRequest request) {
		return problema(HttpStatus.METHOD_NOT_ALLOWED, "Método não permitido", exception.getMessage(), request);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ProblemDetail> tipoDeMidiaNaoSuportado(
			HttpMediaTypeNotSupportedException exception,
			WebRequest request) {
		return problema(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de mídia não suportado",
				exception.getMessage(), request);
	}

	@ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
	public ResponseEntity<ProblemDetail> tipoDeMidiaNaoAceitavel(
			HttpMediaTypeNotAcceptableException exception,
			WebRequest request) {
		return problema(HttpStatus.NOT_ACCEPTABLE, "Tipo de mídia não aceitável",
				exception.getMessage(), request);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ProblemDetail> rotaNaoEncontrada(
			NoResourceFoundException exception,
			WebRequest request) {
		return problema(HttpStatus.NOT_FOUND, "Recurso não encontrado", exception.getMessage(), request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ProblemDetail> erroInesperado(Exception exception, WebRequest request) {
		return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", "Ocorreu um erro inesperado.", request);
	}

	private ResponseEntity<ProblemDetail> problema(
			HttpStatusCode status,
			String title,
			String detail,
			WebRequest request) {
		ProblemDetail problem = criarProblema(status, title, detail);
		adicionarMetadados(problem, request);
		return ResponseEntity.status(status)
				.contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.body(problem);
	}

	private ProblemDetail criarProblema(HttpStatusCode status, String title, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		problem.setProperty("timestamp", Instant.now());
		return problem;
	}

	private void adicionarMetadados(ProblemDetail problem, WebRequest request) {
		if (request instanceof ServletWebRequest servletWebRequest) {
			HttpServletRequest httpRequest = servletWebRequest.getRequest();
			String correlationId = MDC.get("correlationId");
			if (correlationId == null) {
				correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
			}
			if (correlationId != null && !correlationId.isBlank()) {
				problem.setProperty("correlationId", correlationId);
			}

			Object pathVariables = httpRequest.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
			if (pathVariables instanceof Map<?, ?> variables && variables.get("id") != null) {
				if (!problem.getProperties().containsKey("operacaoId")) {
					problem.setProperty("operacaoId", variables.get("id"));
				}
			}
		}
		if (!problem.getProperties().containsKey("timestamp")) {
			problem.setProperty("timestamp", Instant.now());
		}
	}

	private String detalheValidacao(FieldError fieldError) {
		return fieldError.getField() + ": " + fieldError.getDefaultMessage();
	}
}

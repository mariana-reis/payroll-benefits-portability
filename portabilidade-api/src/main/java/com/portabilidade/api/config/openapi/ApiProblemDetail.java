package com.portabilidade.api.config.openapi;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ProblemDetail", description = "Resposta de erro no formato application/problem+json.")
public record ApiProblemDetail(
		@Schema(description = "URI que identifica o tipo de problema.", example = "about:blank")
		String type,
		@Schema(description = "Resumo legível do problema.", example = "Central indisponível")
		String title,
		@Schema(description = "Código HTTP da resposta.", example = "503")
		Integer status,
		@Schema(description = "Explicação específica desta ocorrência.", example = "A Central está temporariamente indisponível.")
		String detail,
		@Schema(description = "URI do recurso que originou o problema.", example = "/api/v1/portabilidade/agenda/BEN_ERRO")
		String instance,
		@Schema(description = "Instante em que o problema foi gerado.", example = "2026-11-08T20:35:00Z")
		Instant timestamp,
		@Schema(description = "Identificador da operação, quando aplicável.", example = "b145b246-1c70-4e16-bc20-90104efecb64")
		String operacaoId,
		@Schema(description = "Identificador de correlação da requisição, quando disponível.", example = "corr-ben-erro")
		String correlationId) {
}

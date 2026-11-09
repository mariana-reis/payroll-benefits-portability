package com.portabilidade.api.operacao.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.portabilidade.api.operacao.entity.StatusOperacao;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado da aceitação de uma solicitação de portabilidade.")
public record SolicitacaoResponse(
		@Schema(description = "Identificador local da operação.", example = "b145b246-1c70-4e16-bc20-90104efecb64")
		UUID operacaoId,
		@Schema(description = "Protocolo retornado pela Central.", example = "protocolo-ben001")
		String protocolo,
		@Schema(description = "Estado inicial ou atual da operação.", example = "PENDENTE")
		StatusOperacao status,
		@Schema(description = "Total calculado pela API a partir das parcelas.", example = "300.00")
		BigDecimal valorTotal,
		@Schema(description = "Data e hora de criação.", example = "2026-11-08T20:35:00Z")
		Instant criadoEm) {
}

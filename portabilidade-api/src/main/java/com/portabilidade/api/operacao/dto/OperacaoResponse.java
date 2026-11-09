package com.portabilidade.api.operacao.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.portabilidade.api.operacao.entity.StatusOperacao;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estado atual da operação e sua trilha de eventos.")
public record OperacaoResponse(
		@Schema(description = "Identificador local da operação.", example = "b145b246-1c70-4e16-bc20-90104efecb64")
		UUID id,
		@Schema(description = "Identificador do beneficiário.", example = "BEN001")
		String beneficiarioId,
		@Schema(description = "Protocolo da Central, quando recebido.", example = "protocolo-ben001")
		String protocoloCentral,
		@Schema(description = "Estado atual da operação.", example = "CONFIRMADA")
		StatusOperacao status,
		@Schema(description = "Motivo apresentado pela Central quando a operação é rejeitada.", example = "Beneficiário atingiu o limite de margem consignável")
		String motivoRejeicao,
		@Schema(description = "Valor total calculado pela API.", example = "300.00")
		BigDecimal valorTotal,
		@Schema(description = "Tempo sugerido até a próxima consulta, quando pendente.", example = "2")
		Long retryAfterSeconds,
		@Schema(description = "Data e hora de criação.", example = "2026-11-08T20:35:00Z")
		Instant criadoEm,
		@Schema(description = "Data e hora da última atualização.", example = "2026-11-08T20:35:10Z")
		Instant atualizadoEm,
		@Schema(description = "Eventos da operação em ordem cronológica.")
		List<EventoResponse> eventos) {

	public OperacaoResponse {
		eventos = List.copyOf(eventos);
	}
}

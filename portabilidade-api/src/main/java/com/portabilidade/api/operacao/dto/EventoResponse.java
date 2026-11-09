package com.portabilidade.api.operacao.dto;

import java.time.Instant;
import java.util.UUID;

import com.portabilidade.api.operacao.entity.StatusOperacao;
import com.portabilidade.api.operacao.entity.TipoEventoOperacao;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Evento imutável do histórico da operação.")
public record EventoResponse(
		@Schema(description = "Identificador do evento.", example = "7e048b4f-a578-42cc-97d0-750507000a2c")
		UUID id,
		@Schema(description = "Tipo de evento registrado.", example = "OPERACAO_CONFIRMADA")
		TipoEventoOperacao tipoEvento,
		@Schema(description = "Estado anterior, quando aplicável.", example = "PENDENTE")
		StatusOperacao statusAnterior,
		@Schema(description = "Estado após o evento, quando aplicável.", example = "CONFIRMADA")
		StatusOperacao statusNovo,
		@Schema(description = "Descrição legível do evento.", example = "Operação confirmada pela Central.")
		String detalhe,
		@Schema(description = "Data e hora em que o evento foi registrado.", example = "2026-11-08T20:35:10Z")
		Instant registradoEm) {
}

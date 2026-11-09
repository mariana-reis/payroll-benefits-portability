package com.portabilidade.api.agenda.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Parcela de desconto apresentada na agenda.")
public record ParcelaResponse(
		@Schema(description = "Identificador da parcela.", example = "P001")
		String parcelaId,
		@Schema(description = "Identificador do convênio.", example = "CONV01")
		String convenioId,
		@Schema(description = "Valor monetário da parcela.", example = "150.00")
		BigDecimal valor,
		@Schema(description = "Data prevista para o desconto.", example = "2026-12-10")
		LocalDate dataDesconto,
		@Schema(description = "Estado atual da parcela.", example = "ABERTA")
		String status) {
}

package com.portabilidade.api.central.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ParcelaResponse(
		String parcelaId,
		String convenioId,
		BigDecimal valor,
		LocalDate dataDesconto,
		String status) {
}

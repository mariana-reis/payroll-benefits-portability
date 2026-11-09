package com.portabilidade.api.autorizacao.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Confirmação da autorização do beneficiário.")
public record OptInResponse(
		@Schema(description = "Identificador do beneficiário.", example = "BEN001")
		String beneficiarioId,
		@Schema(description = "Identificador da autorização registrada.", example = "auth-ben001")
		String autorizacaoId,
		@Schema(description = "Data e hora do registro.", example = "2026-11-08T20:35:00Z")
		Instant criadoEm) {
}

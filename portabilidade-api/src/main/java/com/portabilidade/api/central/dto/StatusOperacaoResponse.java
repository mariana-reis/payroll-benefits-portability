package com.portabilidade.api.central.dto;

public record StatusOperacaoResponse(
		String status,
		String motivoRejeicao,
		Long retryAfterSeconds) {
}

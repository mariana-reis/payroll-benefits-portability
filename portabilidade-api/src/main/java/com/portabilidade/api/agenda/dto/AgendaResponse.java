package com.portabilidade.api.agenda.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Parcelas disponíveis para portabilidade.")
public record AgendaResponse(List<ParcelaResponse> parcelas) {

	public AgendaResponse {
		parcelas = List.copyOf(parcelas);
	}
}

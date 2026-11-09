package com.portabilidade.api.central.dto;

import java.util.List;

public record AgendaResponse(List<ParcelaResponse> parcelas) {

	public AgendaResponse {
		parcelas = List.copyOf(parcelas);
	}
}

package com.portabilidade.api.operacao.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados para solicitar a portabilidade das parcelas selecionadas.")
public record SolicitarPortabilidadeRequest(
		@Schema(description = "Identificador do beneficiário autorizado.", example = "BEN001", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank @Size(max = 100) String beneficiarioId,
		@Schema(description = "Identificadores das parcelas selecionadas.", example = "[\"P001\",\"P002\"]", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotEmpty List<@NotBlank @Size(max = 255) String> parcelaIds,
		@Schema(description = "Conta de destino informada para a portabilidade.", example = "conta-destino-demo", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank @Size(max = 255) String contaDestino) {
}

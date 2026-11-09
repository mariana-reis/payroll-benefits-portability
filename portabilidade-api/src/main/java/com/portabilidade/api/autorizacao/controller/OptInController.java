package com.portabilidade.api.autorizacao.controller;

import com.portabilidade.api.autorizacao.dto.OptInResponse;
import com.portabilidade.api.autorizacao.service.OptInService;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/portabilidade")
@Tag(name = "Autorização", description = "Registro da autorização do beneficiário.")
public class OptInController {

	private final OptInService optInService;

	public OptInController(OptInService optInService) {
		this.optInService = optInService;
	}

	@PostMapping("/opt-in/{beneficiarioId}")
	@Operation(
			summary = "Registrar opt-in",
			description = "Registra ou retorna a autorização já existente para o beneficiário.",
			tags = { "Autorização" })
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Autorização registrada ou reutilizada.",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = OptInResponse.class),
							examples = {
									@ExampleObject(name = "BEN001", value = """
											{"beneficiarioId":"BEN001","autorizacaoId":"auth-ben001","criadoEm":"2026-11-08T20:35:00Z"}
											"""),
									@ExampleObject(name = "BEN002", value = """
											{"beneficiarioId":"BEN002","autorizacaoId":"auth-ben002","criadoEm":"2026-11-08T20:35:00Z"}
											""")
							})),
			@ApiResponse(responseCode = "400", description = "Identificador do beneficiário inválido.",
					content = @Content(mediaType = "application/problem+json",
							schema = @Schema(implementation = com.portabilidade.api.config.openapi.ApiProblemDetail.class),
							examples = @ExampleObject(name = "RequisicaoInvalida", value = """
									{"type":"about:blank","title":"Requisição inválida","status":400,"detail":"beneficiarioId: must not be blank","instance":"/api/v1/portabilidade/opt-in/BEN001","timestamp":"2026-11-08T20:35:00Z","correlationId":"corr-ben001"}
									"""))),
			@ApiResponse(responseCode = "502", description = "Falha inesperada na comunicação com a Central.",
					content = @Content(mediaType = "application/problem+json",
							schema = @Schema(implementation = com.portabilidade.api.config.openapi.ApiProblemDetail.class),
							examples = @ExampleObject(name = "FalhaDeIntegracao", value = """
									{"type":"about:blank","title":"Falha na comunicação com a Central","status":502,"detail":"A Central retornou uma resposta inválida.","instance":"/api/v1/portabilidade/opt-in/BEN001","timestamp":"2026-11-08T20:35:00Z","correlationId":"corr-ben001"}
									"""))),
			@ApiResponse(responseCode = "503", description = "Central indisponível.",
					content = @Content(mediaType = "application/problem+json",
							schema = @Schema(implementation = com.portabilidade.api.config.openapi.ApiProblemDetail.class),
							examples = @ExampleObject(name = "BEN_ERRO", value = """
									{"type":"about:blank","title":"Central indisponível","status":503,"detail":"A Central está temporariamente indisponível.","instance":"/api/v1/portabilidade/opt-in/BEN_ERRO","timestamp":"2026-11-08T20:35:00Z","correlationId":"corr-ben-erro"}
									""")))
	})
	public OptInResponse registrar(
			@Parameter(description = "Identificador do beneficiário.", example = "BEN001", required = true)
			@PathVariable @NotBlank String beneficiarioId) {
		return optInService.registrar(beneficiarioId);
	}
}

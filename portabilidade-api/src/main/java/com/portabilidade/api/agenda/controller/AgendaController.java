package com.portabilidade.api.agenda.controller;

import com.portabilidade.api.agenda.dto.AgendaResponse;
import com.portabilidade.api.agenda.service.AgendaService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/portabilidade")
@Tag(name = "Agenda", description = "Consulta das parcelas disponíveis para portabilidade.")
public class AgendaController {

	private final AgendaService agendaService;

	public AgendaController(AgendaService agendaService) {
		this.agendaService = agendaService;
	}

	@GetMapping("/agenda/{beneficiarioId}")
	@Operation(
			summary = "Consultar agenda",
			description = "Consulta parcelas do beneficiário. É necessário possuir opt-in registrado previamente.",
			tags = { "Agenda" })
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Agenda consultada.",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = AgendaResponse.class),
							examples = {
									@ExampleObject(name = "BEN001", value = """
											{"parcelas":[{"parcelaId":"P001","convenioId":"CONV01","valor":150.00,"dataDesconto":"2026-01-10","status":"ABERTA"},{"parcelaId":"P002","convenioId":"CONV01","valor":150.00,"dataDesconto":"2026-02-10","status":"ABERTA"}]}
											"""),
									@ExampleObject(name = "BEN002", value = """
											{"parcelas":[{"parcelaId":"P010","convenioId":"CONV02","valor":300.00,"dataDesconto":"2026-01-15","status":"ABERTA"}]}
											""")
							})),
			@ApiResponse(responseCode = "400", description = "Identificador do beneficiário inválido.",
					content = @Content(mediaType = "application/problem+json",
							schema = @Schema(implementation = com.portabilidade.api.config.openapi.ApiProblemDetail.class),
							examples = @ExampleObject(name = "RequisicaoInvalida", value = """
									{"type":"about:blank","title":"Requisição inválida","status":400,"detail":"beneficiarioId: must not be blank","instance":"/api/v1/portabilidade/agenda/BEN001","timestamp":"2026-11-08T20:35:00Z","correlationId":"corr-ben001"}
									"""))),
			@ApiResponse(responseCode = "409", description = "Beneficiário ainda não autorizou o acesso à agenda.",
					content = @Content(mediaType = "application/problem+json",
							schema = @Schema(implementation = com.portabilidade.api.config.openapi.ApiProblemDetail.class),
							examples = @ExampleObject(name = "SemAutorizacao", value = """
									{"type":"about:blank","title":"Autorização obrigatória","status":409,"detail":"O beneficiário BEN001 deve autorizar antes de consultar a agenda.","instance":"/api/v1/portabilidade/agenda/BEN001","timestamp":"2026-11-08T20:35:00Z","correlationId":"corr-ben001"}
									"""))),
			@ApiResponse(responseCode = "502", description = "Falha inesperada na comunicação com a Central.",
					content = @Content(mediaType = "application/problem+json",
							schema = @Schema(implementation = com.portabilidade.api.config.openapi.ApiProblemDetail.class),
							examples = @ExampleObject(name = "FalhaDeIntegracao", value = """
									{"type":"about:blank","title":"Falha na comunicação com a Central","status":502,"detail":"A Central retornou uma resposta inválida.","instance":"/api/v1/portabilidade/agenda/BEN001","timestamp":"2026-11-08T20:35:00Z","correlationId":"corr-ben001"}
									"""))),
			@ApiResponse(responseCode = "503", description = "Central indisponível.",
					content = @Content(mediaType = "application/problem+json",
							schema = @Schema(implementation = com.portabilidade.api.config.openapi.ApiProblemDetail.class),
							examples = @ExampleObject(name = "BEN_ERRO", value = """
									{"type":"about:blank","title":"Central indisponível","status":503,"detail":"A Central está temporariamente indisponível.","instance":"/api/v1/portabilidade/agenda/BEN_ERRO","timestamp":"2026-11-08T20:35:00Z","correlationId":"corr-ben-erro"}
									""")))
	})
	public AgendaResponse consultar(
			@Parameter(description = "Identificador do beneficiário autorizado.", example = "BEN001", required = true)
			@PathVariable @NotBlank String beneficiarioId) {
		return agendaService.consultar(beneficiarioId);
	}
}

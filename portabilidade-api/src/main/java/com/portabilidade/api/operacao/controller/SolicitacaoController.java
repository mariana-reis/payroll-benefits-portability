package com.portabilidade.api.operacao.controller;

import com.portabilidade.api.operacao.dto.SolicitarPortabilidadeRequest;
import com.portabilidade.api.operacao.dto.SolicitacaoResponse;
import com.portabilidade.api.operacao.service.SolicitacaoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/portabilidade")
@Tag(name = "Portabilidade", description = "Solicitação idempotente de portabilidade.")
public class SolicitacaoController {

	private final SolicitacaoService solicitacaoService;

	public SolicitacaoController(SolicitacaoService solicitacaoService) {
		this.solicitacaoService = solicitacaoService;
	}

	@PostMapping("/solicitar")
	@Operation(
			summary = "Solicitar portabilidade",
			description = "Valida autorização e parcelas, calcula o total no backend e inicia a operação. Reenvios intencionais devem reutilizar a mesma chave idempotente.",
			tags = { "Portabilidade" })
	@ApiResponses({
			@ApiResponse(responseCode = "202", description = "Solicitação aceita para processamento.",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = SolicitacaoResponse.class),
							examples = {
									@ExampleObject(name = "BEN001", value = """
											{"operacaoId":"b145b246-1c70-4e16-bc20-90104efecb64","protocolo":"protocolo-ben001","status":"PENDENTE","valorTotal":300.00,"criadoEm":"2026-11-08T20:35:00Z"}
											"""),
									@ExampleObject(name = "BEN002", value = """
											{"operacaoId":"b245b246-1c70-4e16-bc20-90104efecb65","protocolo":"protocolo-ben002","status":"PENDENTE","valorTotal":300.00,"criadoEm":"2026-11-08T20:35:00Z"}
											""")
							})),
			@ApiResponse(responseCode = "400", description = "Cabeçalho ou corpo inválido.",
					content = @Content(mediaType = "application/problem+json",
							schema = @Schema(implementation = com.portabilidade.api.config.openapi.ApiProblemDetail.class),
							examples = @ExampleObject(name = "RequisicaoInvalida", value = """
									{"type":"about:blank","title":"Requisição inválida","status":400,"detail":"parcelaIds: must not be empty","instance":"/api/v1/portabilidade/solicitar","timestamp":"2026-11-08T20:35:00Z","correlationId":"corr-ben001"}
									"""))),
			@ApiResponse(responseCode = "409", description = "O beneficiário não possui autorização válida.",
					content = @Content(mediaType = "application/problem+json",
							schema = @Schema(implementation = com.portabilidade.api.config.openapi.ApiProblemDetail.class),
							examples = @ExampleObject(name = "SemAutorizacao", value = """
									{"type":"about:blank","title":"Autorização obrigatória","status":409,"detail":"O beneficiário BEN001 deve autorizar antes de solicitar a portabilidade.","instance":"/api/v1/portabilidade/solicitar","timestamp":"2026-11-08T20:35:00Z","correlationId":"corr-ben001"}
									"""))),
			@ApiResponse(responseCode = "422", description = "Solicitação inválida ou chave reutilizada com payload diferente.",
					content = @Content(mediaType = "application/problem+json",
							schema = @Schema(implementation = com.portabilidade.api.config.openapi.ApiProblemDetail.class),
							examples = @ExampleObject(name = "PayloadDiferente", value = """
									{"type":"about:blank","title":"Solicitação inválida","status":422,"detail":"A chave de idempotência já foi utilizada com outro payload.","instance":"/api/v1/portabilidade/solicitar","timestamp":"2026-11-08T20:35:00Z","correlationId":"corr-ben001"}
									"""))),
			@ApiResponse(responseCode = "502", description = "Falha inesperada na comunicação com a Central.",
					content = @Content(mediaType = "application/problem+json",
							schema = @Schema(implementation = com.portabilidade.api.config.openapi.ApiProblemDetail.class),
							examples = @ExampleObject(name = "FalhaDeIntegracao", value = """
									{"type":"about:blank","title":"Falha na comunicação com a Central","status":502,"detail":"A Central retornou uma resposta inválida.","instance":"/api/v1/portabilidade/solicitar","timestamp":"2026-11-08T20:35:00Z","operacaoId":"b145b246-1c70-4e16-bc20-90104efecb64","correlationId":"corr-ben001"}
									"""))),
			@ApiResponse(responseCode = "503", description = "Central indisponível; a operação permanece registrada como FALHA_INTEGRACAO.",
					content = @Content(mediaType = "application/problem+json",
							schema = @Schema(implementation = com.portabilidade.api.config.openapi.ApiProblemDetail.class),
							examples = @ExampleObject(name = "BEN_ERRO", value = """
									{"type":"about:blank","title":"Central indisponível","status":503,"detail":"A Central está temporariamente indisponível.","instance":"/api/v1/portabilidade/solicitar","timestamp":"2026-11-08T20:35:00Z","operacaoId":"c145b246-1c70-4e16-bc20-90104efecb66","correlationId":"corr-ben-erro"}
									""")))
	})
	public ResponseEntity<SolicitacaoResponse> solicitar(
			@Parameter(description = "Chave única da intenção. Reutilize-a somente para reenviar o mesmo payload.", example = "idem-ben001-001", required = true)
			@RequestHeader("X-Idempotency-Key") @NotBlank @Size(max = 255) String chaveIdempotencia,
			@io.swagger.v3.oas.annotations.parameters.RequestBody(
					description = "Beneficiário, parcelas escolhidas e conta de destino. O total monetário é calculado pela API.",
					required = true,
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = SolicitarPortabilidadeRequest.class),
							examples = {
									@ExampleObject(name = "BEN001", value = """
											{"beneficiarioId":"BEN001","parcelaIds":["P001","P002"],"contaDestino":"conta-destino-demo"}
											"""),
									@ExampleObject(name = "BEN002", value = """
											{"beneficiarioId":"BEN002","parcelaIds":["P010"],"contaDestino":"conta-destino-demo"}
											""")
							}))
			@Valid @RequestBody SolicitarPortabilidadeRequest request) {
		return ResponseEntity.status(HttpStatus.ACCEPTED)
				.body(solicitacaoService.solicitar(chaveIdempotencia, request));
	}
}

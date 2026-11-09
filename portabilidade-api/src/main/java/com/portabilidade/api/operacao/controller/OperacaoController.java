package com.portabilidade.api.operacao.controller;

import java.util.UUID;

import com.portabilidade.api.operacao.dto.OperacaoResponse;
import com.portabilidade.api.operacao.service.StatusOperacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portabilidade")
@Tag(name = "Operação", description = "Consulta do estado e dos eventos de uma operação.")
public class OperacaoController {

	private final StatusOperacaoService statusOperacaoService;

	public OperacaoController(StatusOperacaoService statusOperacaoService) {
		this.statusOperacaoService = statusOperacaoService;
	}

	@GetMapping("/operacao/{id}")
	@Operation(
			summary = "Consultar operação",
			description = "Retorna o estado atual, motivo de rejeição e eventos em ordem cronológica. Operações não terminais podem consultar a Central.",
			tags = { "Operação" })
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Operação encontrada.",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = OperacaoResponse.class),
							examples = {
									@ExampleObject(name = "BEN001", value = """
											{"id":"b145b246-1c70-4e16-bc20-90104efecb64","beneficiarioId":"BEN001","protocoloCentral":"protocolo-ben001","status":"CONFIRMADA","motivoRejeicao":null,"valorTotal":300.00,"retryAfterSeconds":null,"criadoEm":"2026-11-08T20:35:00Z","atualizadoEm":"2026-11-08T20:35:10Z","eventos":[{"id":"7e048b4f-a578-42cc-97d0-750507000a2c","tipoEvento":"OPERACAO_CONFIRMADA","statusAnterior":"PENDENTE","statusNovo":"CONFIRMADA","detalhe":"Operação confirmada pela Central.","registradoEm":"2026-11-08T20:35:10Z"}]}
											"""),
									@ExampleObject(name = "BEN002", value = """
											{"id":"b245b246-1c70-4e16-bc20-90104efecb65","beneficiarioId":"BEN002","protocoloCentral":"protocolo-ben002","status":"REJEITADA","motivoRejeicao":"Beneficiário atingiu o limite de margem consignável","valorTotal":300.00,"retryAfterSeconds":null,"criadoEm":"2026-11-08T20:35:00Z","atualizadoEm":"2026-11-08T20:35:10Z","eventos":[{"id":"8e048b4f-a578-42cc-97d0-750507000a2d","tipoEvento":"OPERACAO_REJEITADA","statusAnterior":"PENDENTE","statusNovo":"REJEITADA","detalhe":"Beneficiário atingiu o limite de margem consignável","registradoEm":"2026-11-08T20:35:10Z"}]}
											""")
							})),
			@ApiResponse(responseCode = "400", description = "Identificador da operação inválido.",
					content = @Content(mediaType = "application/problem+json",
							schema = @Schema(implementation = com.portabilidade.api.config.openapi.ApiProblemDetail.class),
							examples = @ExampleObject(name = "RequisicaoInvalida", value = """
									{"type":"about:blank","title":"Requisição inválida","status":400,"detail":"O identificador da operação deve ser um UUID válido.","instance":"/api/v1/portabilidade/operacao/invalid","timestamp":"2026-11-08T20:35:00Z","correlationId":"corr-ben001"}
									"""))),
			@ApiResponse(responseCode = "404", description = "Operação ou protocolo inexistente.",
					content = @Content(mediaType = "application/problem+json",
							schema = @Schema(implementation = com.portabilidade.api.config.openapi.ApiProblemDetail.class),
							examples = @ExampleObject(name = "OperacaoNaoEncontrada", value = """
									{"type":"about:blank","title":"Operação não encontrada","status":404,"detail":"A operação solicitada não foi encontrada.","instance":"/api/v1/portabilidade/operacao/b145b246-1c70-4e16-bc20-90104efecb64","timestamp":"2026-11-08T20:35:00Z","operacaoId":"b145b246-1c70-4e16-bc20-90104efecb64","correlationId":"corr-ben001"}
									"""))),
			@ApiResponse(responseCode = "502", description = "Falha inesperada na comunicação com a Central.",
					content = @Content(mediaType = "application/problem+json",
							schema = @Schema(implementation = com.portabilidade.api.config.openapi.ApiProblemDetail.class),
							examples = @ExampleObject(name = "FalhaDeIntegracao", value = """
									{"type":"about:blank","title":"Falha na comunicação com a Central","status":502,"detail":"A Central retornou uma resposta inválida.","instance":"/api/v1/portabilidade/operacao/b145b246-1c70-4e16-bc20-90104efecb64","timestamp":"2026-11-08T20:35:00Z","operacaoId":"b145b246-1c70-4e16-bc20-90104efecb64","correlationId":"corr-ben001"}
									"""))),
			@ApiResponse(responseCode = "503", description = "Central indisponível.",
					content = @Content(mediaType = "application/problem+json",
							schema = @Schema(implementation = com.portabilidade.api.config.openapi.ApiProblemDetail.class),
							examples = @ExampleObject(name = "BEN_ERRO", value = """
									{"type":"about:blank","title":"Central indisponível","status":503,"detail":"A Central está temporariamente indisponível.","instance":"/api/v1/portabilidade/operacao/c145b246-1c70-4e16-bc20-90104efecb66","timestamp":"2026-11-08T20:35:00Z","operacaoId":"c145b246-1c70-4e16-bc20-90104efecb66","correlationId":"corr-ben-erro"}
									""")))
	})
	public OperacaoResponse consultar(
			@Parameter(description = "UUID local da operação.", example = "b145b246-1c70-4e16-bc20-90104efecb64", required = true)
			@PathVariable UUID id) {
		return statusOperacaoService.consultar(id);
	}
}

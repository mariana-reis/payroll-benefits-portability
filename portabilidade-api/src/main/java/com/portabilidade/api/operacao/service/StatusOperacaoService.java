package com.portabilidade.api.operacao.service;

import java.util.UUID;

import com.portabilidade.api.central.client.CentralClient;
import com.portabilidade.api.central.dto.StatusOperacaoResponse;
import com.portabilidade.api.central.exception.CentralClientException;
import com.portabilidade.api.central.exception.CentralIndisponivelException;
import com.portabilidade.api.operacao.dto.OperacaoResponse;
import com.portabilidade.api.operacao.event.OperacaoEventoService;
import com.portabilidade.api.operacao.entity.OperacaoPortabilidade;
import com.portabilidade.api.operacao.entity.StatusOperacao;
import com.portabilidade.api.operacao.exception.CentralClientOperacaoException;
import com.portabilidade.api.operacao.exception.CentralIndisponivelOperacaoException;
import com.portabilidade.api.operacao.exception.OperacaoNaoEncontradaException;
import com.portabilidade.api.operacao.repository.OperacaoPortabilidadeRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
public class StatusOperacaoService {

	private final OperacaoPortabilidadeRepository operacaoRepository;
	private final StatusPersistenciaService persistenciaService;
	private final CentralClient centralClient;
	private final OperacaoEventoService eventoService;

	public StatusOperacaoService(
			OperacaoPortabilidadeRepository operacaoRepository,
			StatusPersistenciaService persistenciaService,
			CentralClient centralClient,
			OperacaoEventoService eventoService) {
		this.operacaoRepository = operacaoRepository;
		this.persistenciaService = persistenciaService;
		this.centralClient = centralClient;
		this.eventoService = eventoService;
	}

	public OperacaoResponse consultar(UUID operacaoId) {
		OperacaoPortabilidade operacao = operacaoRepository.findWithAutorizacaoById(operacaoId)
				.orElseThrow(() -> new OperacaoNaoEncontradaException(operacaoId));
		if (estadoTerminal(operacao.getStatus())) {
			return paraResponse(operacao, null);
		}
		if (operacao.getProtocoloCentral() == null || operacao.getProtocoloCentral().isBlank()) {
			if (operacao.getStatus() == StatusOperacao.FALHA_INTEGRACAO) {
				return paraResponse(operacao, null);
			}
			throw new CentralClientException("A operação ainda não possui protocolo da Central.");
		}

		StatusOperacaoResponse respostaCentral;
		StatusOperacao novoStatus;
		try {
			respostaCentral = centralClient.consultarStatus(operacao.getProtocoloCentral());
			if (respostaCentral == null) {
				throw new CentralClientException("A Central não retornou o status da operação.");
			}
			novoStatus = respostaCentral.status() == null
					? null
					: converterStatusCentral(respostaCentral.status());
		} catch (CentralIndisponivelException exception) {
			eventoService.registrarFalhaIntegracao(operacao, "Falha na consulta de status na Central.");
			throw new CentralIndisponivelOperacaoException(operacaoId, exception);
		} catch (CentralClientException exception) {
			eventoService.registrarFalhaIntegracao(operacao, "Falha na consulta de status na Central.");
			throw new CentralClientOperacaoException(operacaoId, exception);
		}
		if (respostaCentral.status() == null) {
			return paraResponse(operacao, respostaCentral.retryAfterSeconds());
		}

		if (novoStatus == null) {
			return paraResponse(operacao, null);
		}

		try {
			OperacaoPortabilidade atualizada = persistenciaService.aplicarTransicao(
					operacaoId,
					novoStatus,
					respostaCentral.motivoRejeicao());
			return paraResponse(operacao, atualizada, null);
		} catch (ObjectOptimisticLockingFailureException conflitoConcorrente) {
			OperacaoPortabilidade atual = persistenciaService.buscar(operacaoId)
					.orElseThrow(() -> new OperacaoNaoEncontradaException(operacaoId));
			return paraResponse(atual, null);
		}
	}

	private StatusOperacao converterStatusCentral(String status) {
		try {
			StatusOperacao estado = StatusOperacao.valueOf(status);
			if (estado != StatusOperacao.CONFIRMADA && estado != StatusOperacao.REJEITADA) {
				throw new CentralClientException("A Central retornou um status terminal inválido: " + status + ".");
			}
			return estado;
		} catch (IllegalArgumentException exception) {
			throw new CentralClientException("A Central retornou um status desconhecido: " + status + ".", exception);
		}
	}

	private boolean estadoTerminal(StatusOperacao status) {
		return status == StatusOperacao.CONFIRMADA || status == StatusOperacao.REJEITADA;
	}

	private OperacaoResponse paraResponse(OperacaoPortabilidade operacao, Long retryAfterSeconds) {
		return paraResponse(operacao, operacao, retryAfterSeconds);
	}

	private OperacaoResponse paraResponse(OperacaoPortabilidade beneficiarioOrigem,
			OperacaoPortabilidade dadosAtualizados,
			Long retryAfterSeconds) {
		return new OperacaoResponse(
				dadosAtualizados.getId(),
				beneficiarioOrigem.getAutorizacao().getBeneficiarioId(),
				dadosAtualizados.getProtocoloCentral(),
				dadosAtualizados.getStatus(),
				dadosAtualizados.getMotivoRejeicao(),
				dadosAtualizados.getValorTotal(),
				retryAfterSeconds,
				dadosAtualizados.getCriadoEm(),
				dadosAtualizados.getAtualizadoEm(),
				eventoService.consultar(dadosAtualizados.getId()));
	}
}

package com.portabilidade.api.operacao.service;

import java.util.Optional;
import java.util.UUID;

import com.portabilidade.api.operacao.domain.MaquinaEstadoOperacao;
import com.portabilidade.api.operacao.entity.OperacaoPortabilidade;
import com.portabilidade.api.operacao.entity.StatusOperacao;
import com.portabilidade.api.operacao.event.OperacaoEventoService;
import com.portabilidade.api.operacao.exception.OperacaoNaoEncontradaException;
import com.portabilidade.api.operacao.repository.OperacaoPortabilidadeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatusPersistenciaService {

	private static final Logger LOGGER = LoggerFactory.getLogger(StatusPersistenciaService.class);

	private final OperacaoPortabilidadeRepository operacaoRepository;
	private final MaquinaEstadoOperacao maquinaEstado;
	private final OperacaoEventoService eventoService;

	public StatusPersistenciaService(
			OperacaoPortabilidadeRepository operacaoRepository,
			MaquinaEstadoOperacao maquinaEstado,
			OperacaoEventoService eventoService) {
		this.operacaoRepository = operacaoRepository;
		this.maquinaEstado = maquinaEstado;
		this.eventoService = eventoService;
	}

	@Transactional
	public OperacaoPortabilidade aplicarTransicao(UUID operacaoId, StatusOperacao novoStatus, String motivoRejeicao) {
		OperacaoPortabilidade operacao = operacaoRepository.findById(operacaoId)
				.orElseThrow(() -> new OperacaoNaoEncontradaException(operacaoId));
		if (!maquinaEstado.transicaoPermitida(operacao.getStatus(), novoStatus)) {
			LOGGER.warn("Transição de status inválida ignorada para operação {}: {} -> {}",
					operacaoId, operacao.getStatus(), novoStatus);
			return operacao;
		}

		StatusOperacao statusAnterior = operacao.getStatus();
		operacao.setStatus(novoStatus);
		operacao.setMotivoRejeicao(novoStatus == StatusOperacao.REJEITADA ? motivoRejeicao : null);
		OperacaoPortabilidade atualizada = operacaoRepository.saveAndFlush(operacao);
		if (novoStatus == StatusOperacao.CONFIRMADA) {
			eventoService.registrarConfirmacao(atualizada, statusAnterior);
		} else if (novoStatus == StatusOperacao.REJEITADA) {
			eventoService.registrarRejeicao(atualizada, statusAnterior);
		}
		return atualizada;
	}

	@Transactional
	public OperacaoPortabilidade marcarFalhaIntegracao(UUID operacaoId, String detalhe) {
		OperacaoPortabilidade operacao = operacaoRepository.findById(operacaoId)
				.orElseThrow(() -> new OperacaoNaoEncontradaException(operacaoId));
		if (!maquinaEstado.transicaoPermitida(operacao.getStatus(), StatusOperacao.FALHA_INTEGRACAO)) {
			LOGGER.warn("Transição de falha de integração inválida ignorada para operação {}: {} -> {}",
					operacaoId, operacao.getStatus(), StatusOperacao.FALHA_INTEGRACAO);
			return operacao;
		}

		StatusOperacao statusAnterior = operacao.getStatus();
		operacao.setStatus(StatusOperacao.FALHA_INTEGRACAO);
		OperacaoPortabilidade atualizada = operacaoRepository.saveAndFlush(operacao);
		eventoService.registrarFalhaIntegracao(atualizada, statusAnterior, detalhe);
		return atualizada;
	}

	@Transactional
	public Optional<OperacaoPortabilidade> prepararReprocessamento(UUID operacaoId) {
		OperacaoPortabilidade operacao = operacaoRepository.findById(operacaoId)
				.orElseThrow(() -> new OperacaoNaoEncontradaException(operacaoId));
		if (!maquinaEstado.transicaoPermitida(operacao.getStatus(), StatusOperacao.PENDENTE)) {
			LOGGER.warn("Retentativa de integração ignorada para operação {} em estado {}",
					operacaoId, operacao.getStatus());
			return Optional.empty();
		}

		StatusOperacao statusAnterior = operacao.getStatus();
		operacao.setStatus(StatusOperacao.PENDENTE);
		OperacaoPortabilidade atualizada = operacaoRepository.saveAndFlush(operacao);
		eventoService.registrarRetentativa(atualizada, statusAnterior);
		return Optional.of(atualizada);
	}

	@Transactional(readOnly = true)
	public Optional<OperacaoPortabilidade> buscar(UUID operacaoId) {
		return operacaoRepository.findWithAutorizacaoById(operacaoId);
	}
}

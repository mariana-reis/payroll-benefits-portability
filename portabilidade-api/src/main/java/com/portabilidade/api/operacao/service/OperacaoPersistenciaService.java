package com.portabilidade.api.operacao.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.portabilidade.api.agenda.dto.ParcelaResponse;
import com.portabilidade.api.agenda.exception.AutorizacaoObrigatoriaException;
import com.portabilidade.api.autorizacao.entity.Autorizacao;
import com.portabilidade.api.autorizacao.repository.AutorizacaoRepository;
import com.portabilidade.api.operacao.entity.OperacaoParcela;
import com.portabilidade.api.operacao.entity.OperacaoPortabilidade;
import com.portabilidade.api.operacao.entity.StatusOperacao;
import com.portabilidade.api.operacao.event.OperacaoEventoService;
import com.portabilidade.api.operacao.repository.OperacaoPortabilidadeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperacaoPersistenciaService {

	private final AutorizacaoRepository autorizacaoRepository;
	private final OperacaoPortabilidadeRepository operacaoRepository;
	private final OperacaoEventoService eventoService;

	public OperacaoPersistenciaService(
			AutorizacaoRepository autorizacaoRepository,
			OperacaoPortabilidadeRepository operacaoRepository,
			OperacaoEventoService eventoService) {
		this.autorizacaoRepository = autorizacaoRepository;
		this.operacaoRepository = operacaoRepository;
		this.eventoService = eventoService;
	}

	@Transactional
	public OperacaoPortabilidade criar(
			String beneficiarioId,
			String chaveIdempotencia,
			String hash,
			BigDecimal valorTotal,
			String contaDestino,
			List<ParcelaResponse> parcelas) {
		Autorizacao autorizacao = autorizacaoRepository.findByBeneficiarioId(beneficiarioId)
				.orElseThrow(() -> new AutorizacaoObrigatoriaException(beneficiarioId));

		OperacaoPortabilidade operacao = new OperacaoPortabilidade(
				autorizacao,
				chaveIdempotencia,
				hash,
				valorTotal,
				contaDestino,
				StatusOperacao.PENDENTE);
		parcelas.forEach(parcela -> operacao.adicionarParcela(
				new OperacaoParcela(parcela.parcelaId(), parcela.valor())));
		OperacaoPortabilidade criada = operacaoRepository.saveAndFlush(operacao);
		eventoService.registrarCriacao(criada);
		return criada;
	}

	@Transactional
	public OperacaoPortabilidade atualizarProtocolo(UUID operacaoId, String protocolo) {
		OperacaoPortabilidade operacao = operacaoRepository.findById(operacaoId).orElseThrow();
		operacao.setProtocoloCentral(protocolo);
		OperacaoPortabilidade atualizada = operacaoRepository.saveAndFlush(operacao);
		eventoService.registrarProtocolo(atualizada);
		return atualizada;
	}
}

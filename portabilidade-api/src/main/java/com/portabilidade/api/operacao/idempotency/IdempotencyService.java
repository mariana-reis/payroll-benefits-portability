package com.portabilidade.api.operacao.idempotency;

import java.util.Optional;

import com.portabilidade.api.operacao.entity.OperacaoPortabilidade;
import com.portabilidade.api.operacao.exception.IdempotencyPayloadMismatchException;
import com.portabilidade.api.operacao.repository.OperacaoPortabilidadeRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyService {

	private final OperacaoPortabilidadeRepository operacaoRepository;

	public IdempotencyService(OperacaoPortabilidadeRepository operacaoRepository) {
		this.operacaoRepository = operacaoRepository;
	}

	public Optional<OperacaoPortabilidade> buscarExistente(String chave, String hash) {
		return operacaoRepository.findByIdempotencyKey(chave)
				.map(operacao -> validarPayload(operacao, hash));
	}

	public OperacaoPortabilidade recuperarVencedora(
			String chave,
			String hash,
			DataIntegrityViolationException conflitoPersistencia) {
		return operacaoRepository.findByIdempotencyKey(chave)
				.map(operacao -> validarPayload(operacao, hash))
				.orElseThrow(() -> conflitoPersistencia);
	}

	private OperacaoPortabilidade validarPayload(OperacaoPortabilidade operacao, String hash) {
		if (!operacao.getRequestHash().equals(hash)) {
			throw new IdempotencyPayloadMismatchException();
		}
		return operacao;
	}
}

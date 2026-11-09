package com.portabilidade.api.operacao.exception;

import java.util.UUID;

public class OperacaoNaoEncontradaException extends RuntimeException {

	private final UUID operacaoId;

	public OperacaoNaoEncontradaException(UUID operacaoId) {
		super("A operação " + operacaoId + " não foi encontrada.");
		this.operacaoId = operacaoId;
	}

	public UUID getOperacaoId() {
		return operacaoId;
	}
}

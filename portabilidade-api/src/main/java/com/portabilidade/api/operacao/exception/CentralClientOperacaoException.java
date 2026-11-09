package com.portabilidade.api.operacao.exception;

import java.util.UUID;

import com.portabilidade.api.central.exception.CentralClientException;

public class CentralClientOperacaoException extends CentralClientException {

	private final UUID operacaoId;

	public CentralClientOperacaoException(UUID operacaoId, CentralClientException causa) {
		super(causa.getMessage(), causa);
		this.operacaoId = operacaoId;
	}

	public UUID getOperacaoId() {
		return operacaoId;
	}
}

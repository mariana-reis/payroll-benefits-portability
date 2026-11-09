package com.portabilidade.api.operacao.exception;

import java.util.UUID;

import com.portabilidade.api.central.exception.CentralIndisponivelException;

public class CentralIndisponivelOperacaoException extends CentralIndisponivelException {

	private final UUID operacaoId;

	public CentralIndisponivelOperacaoException(UUID operacaoId, CentralIndisponivelException causa) {
		super(causa.getMessage(), causa);
		this.operacaoId = operacaoId;
	}

	public UUID getOperacaoId() {
		return operacaoId;
	}
}

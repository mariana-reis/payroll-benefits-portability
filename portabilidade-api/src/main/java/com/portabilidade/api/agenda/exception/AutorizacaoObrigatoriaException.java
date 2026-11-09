package com.portabilidade.api.agenda.exception;

public class AutorizacaoObrigatoriaException extends RuntimeException {

	public AutorizacaoObrigatoriaException(String beneficiarioId) {
		super("O beneficiário " + beneficiarioId + " precisa autorizar o acesso antes de consultar a agenda.");
	}
}

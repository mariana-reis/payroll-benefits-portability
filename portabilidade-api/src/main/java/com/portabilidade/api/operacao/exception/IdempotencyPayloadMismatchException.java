package com.portabilidade.api.operacao.exception;

public class IdempotencyPayloadMismatchException extends RuntimeException {

	public IdempotencyPayloadMismatchException() {
		super("A chave de idempotência já foi utilizada com um payload diferente.");
	}
}

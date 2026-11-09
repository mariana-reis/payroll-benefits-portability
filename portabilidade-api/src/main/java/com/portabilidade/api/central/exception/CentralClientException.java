package com.portabilidade.api.central.exception;

public class CentralClientException extends RuntimeException {

	public CentralClientException(String message) {
		super(message);
	}

	public CentralClientException(String message, Throwable cause) {
		super(message, cause);
	}
}

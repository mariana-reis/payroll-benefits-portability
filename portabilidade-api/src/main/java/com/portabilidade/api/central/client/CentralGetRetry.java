package com.portabilidade.api.central.client;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.portabilidade.api.central.exception.CentralIndisponivelException;
import com.portabilidade.api.central.properties.CentralRetryProperties;

public class CentralGetRetry {

	private final CentralRetryProperties properties;
	private final Consumer<Duration> waiter;

	public CentralGetRetry(CentralRetryProperties properties) {
		this(properties, CentralGetRetry::sleep);
	}

	CentralGetRetry(CentralRetryProperties properties, Consumer<Duration> waiter) {
		this.properties = Objects.requireNonNull(properties);
		this.waiter = Objects.requireNonNull(waiter);
	}

	public <T> T executar(Supplier<T> consulta) {
		Duration backoff = properties.initialBackoff();
		for (int tentativa = 1; ; tentativa++) {
			try {
				return consulta.get();
			} catch (CentralIndisponivelException exception) {
				if (tentativa >= properties.maxAttempts()) {
					throw exception;
				}
				aguardar(backoff);
				backoff = proximoBackoff(backoff);
			}
		}
	}

	private Duration proximoBackoff(Duration atual) {
		if (atual.isZero() || atual.compareTo(properties.maxBackoff().dividedBy(properties.multiplier())) >= 0) {
			return properties.maxBackoff();
		}
		return atual.multipliedBy(properties.multiplier());
	}

	private void aguardar(Duration backoff) {
		waiter.accept(backoff);
	}

	private static void sleep(Duration backoff) {
		try {
			long millis = backoff.toMillis();
			int nanos = (int) (backoff.minusMillis(millis).toNanos());
			Thread.sleep(millis, nanos);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new CentralIndisponivelException(
					"Consulta à Central interrompida durante o backoff de retry.", exception);
		}
	}
}

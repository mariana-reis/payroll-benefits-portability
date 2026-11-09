package com.portabilidade.api.central.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.portabilidade.api.central.exception.CentralIndisponivelException;
import com.portabilidade.api.central.properties.CentralRetryProperties;
import org.junit.jupiter.api.Test;

class CentralGetRetryTests {

	@Test
	void deveAplicarBackoffExponencialComLimite() {
		List<Duration> esperas = new ArrayList<>();
		CentralGetRetry retry = new CentralGetRetry(
				new CentralRetryProperties(4, Duration.ofMillis(100), Duration.ofMillis(300), 2),
				esperas::add);
		AtomicInteger chamadas = new AtomicInteger();

		String resultado = retry.executar(() -> {
			if (chamadas.getAndIncrement() < 3) {
				throw new CentralIndisponivelException("Indisponível", new IllegalStateException());
			}
			return "ok";
		});

		assertEquals("ok", resultado);
		assertEquals(4, chamadas.get());
		assertEquals(List.of(
				Duration.ofMillis(100),
				Duration.ofMillis(200),
				Duration.ofMillis(300)), esperas);
	}
}

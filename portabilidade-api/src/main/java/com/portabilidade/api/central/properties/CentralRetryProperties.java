package com.portabilidade.api.central.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "central.retry")
public record CentralRetryProperties(
		int maxAttempts,
		Duration initialBackoff,
		Duration maxBackoff,
		int multiplier) {

	public CentralRetryProperties {
		if (maxAttempts < 1 || initialBackoff.isNegative() || maxBackoff.isNegative()
				|| multiplier < 1 || maxBackoff.compareTo(initialBackoff) < 0) {
			throw new IllegalArgumentException("A configuração de retry da Central é inválida.");
		}
	}
}

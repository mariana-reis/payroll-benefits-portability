package com.portabilidade.api.central.properties;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "central")
public record CentralProperties(
		URI baseUrl,
		Duration connectTimeout,
		Duration readTimeout,
		String hmacSecret) {
}

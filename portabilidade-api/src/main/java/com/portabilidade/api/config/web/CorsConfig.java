package com.portabilidade.api.config.web;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
public class CorsConfig implements WebMvcConfigurer {

	private final List<String> allowedOrigins;

	public CorsConfig(@Value("${app.cors.allowed-origins}") String allowedOrigins) {
		this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
				.map(origin -> Objects.requireNonNull(origin).trim())
				.filter(origin -> !origin.isEmpty())
				.toList();
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
				.allowedOrigins(allowedOrigins.toArray(String[]::new))
				.allowedMethods("GET", "POST", "OPTIONS")
				.allowedHeaders(
						"Content-Type",
						"X-Idempotency-Key",
						"X-Correlation-Id",
						"X-Timestamp",
						"X-Signature")
				.exposedHeaders("Retry-After", "X-Correlation-Id")
				.allowCredentials(false);
	}
}

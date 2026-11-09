package com.portabilidade.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.time.Duration;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.portabilidade.api.central.properties.CentralProperties;

@SpringBootTest(properties = {
		"central.base-url=http://localhost:3000",
		"central.connect-timeout=2s",
		"central.read-timeout=5s",
		"central.hmac-secret=test-secret",
		"central.retry.initial-backoff=0ms",
		"central.retry.max-backoff=0ms",
		"central.retry.max-attempts=1"
})
@AutoConfigureMockMvc
class PortabilidadeApiApplicationTests {

	@Autowired
	private CentralProperties centralProperties;

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
		assertEquals(URI.create("http://localhost:3000"), centralProperties.baseUrl());
		assertEquals(Duration.ofSeconds(2), centralProperties.connectTimeout());
		assertEquals(Duration.ofSeconds(5), centralProperties.readTimeout());
	}

	@Test
	void correlationIdGeradoEActuatorDisponivel() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(header().string("X-Correlation-Id", matchesPattern(
						"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")));

		mockMvc.perform(MockMvcRequestBuilders.get("/actuator/info"))
				.andExpect(status().isOk());
	}

	@Test
	void correlationIdValidoEhReutilizado() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/actuator/health")
						.header("X-Correlation-Id", "corr-existente-123"))
				.andExpect(status().isOk())
				.andExpect(header().string("X-Correlation-Id", "corr-existente-123"));
	}

}

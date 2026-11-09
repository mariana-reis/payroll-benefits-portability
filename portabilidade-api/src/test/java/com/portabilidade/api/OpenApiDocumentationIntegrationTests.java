package com.portabilidade.api;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest(properties = {
		"central.base-url=http://localhost:3000",
		"central.hmac-secret=test-secret",
		"central.retry.initial-backoff=0ms",
		"central.retry.max-backoff=0ms",
		"central.retry.max-attempts=1"
})
@AutoConfigureMockMvc
class OpenApiDocumentationIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void deveGerarContratoComEndpointsSchemasRespostasEExemplos() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$['paths']['/api/v1/portabilidade/opt-in/{beneficiarioId}'].post").exists())
				.andExpect(jsonPath("$['paths']['/api/v1/portabilidade/agenda/{beneficiarioId}'].get").exists())
				.andExpect(jsonPath("$['paths']['/api/v1/portabilidade/solicitar'].post").exists())
				.andExpect(jsonPath("$['paths']['/api/v1/portabilidade/operacao/{id}'].get").exists())
				.andExpect(jsonPath("$.components.schemas.OptInResponse").exists())
				.andExpect(jsonPath("$.components.schemas.AgendaResponse").exists())
				.andExpect(jsonPath("$.components.schemas.ParcelaResponse").exists())
				.andExpect(jsonPath("$.components.schemas.SolicitarPortabilidadeRequest").exists())
				.andExpect(jsonPath("$.components.schemas.SolicitacaoResponse").exists())
				.andExpect(jsonPath("$.components.schemas.OperacaoResponse").exists())
				.andExpect(jsonPath("$.components.schemas.EventoResponse").exists())
				.andExpect(jsonPath("$.components.schemas.ProblemDetail.properties.timestamp").exists())
				.andExpect(jsonPath("$.components.schemas.ProblemDetail.properties.operacaoId").exists())
				.andExpect(jsonPath("$.components.schemas.ProblemDetail.properties.correlationId").exists())
				.andExpect(jsonPath("$.components.schemas.ProblemDetail.properties.type").exists())
				.andExpect(jsonPath("$.components.schemas.ProblemDetail.properties.title").exists())
				.andExpect(jsonPath("$.components.schemas.ProblemDetail.properties.status").exists())
				.andExpect(jsonPath("$.components.schemas.ProblemDetail.properties.detail").exists())
				.andExpect(jsonPath("$.tags[?(@.name == 'Autorização')]").exists())
				.andExpect(jsonPath("$.tags[?(@.name == 'Agenda')]").exists())
				.andExpect(jsonPath("$.tags[?(@.name == 'Portabilidade')]").exists())
				.andExpect(jsonPath("$.tags[?(@.name == 'Operação')]").exists())
				.andExpect(jsonPath(
						"$['paths']['/api/v1/portabilidade/agenda/{beneficiarioId}'].get.responses['503'].content['application/problem+json'].examples.BEN_ERRO")
						.exists())
				.andExpect(jsonPath(
						"$['paths']['/api/v1/portabilidade/agenda/{beneficiarioId}'].get.responses['200'].content['application/json'].examples.BEN001")
						.exists())
				.andExpect(jsonPath(
						"$['paths']['/api/v1/portabilidade/agenda/{beneficiarioId}'].get.responses['200'].content['application/json'].examples.BEN002")
						.exists())
				.andExpect(jsonPath(
						"$['paths']['/api/v1/portabilidade/solicitar'].post.responses['400'].content['application/problem+json']")
						.exists())
				.andExpect(jsonPath(
						"$['paths']['/api/v1/portabilidade/agenda/{beneficiarioId}'].get.responses['409'].content['application/problem+json']")
						.exists())
				.andExpect(jsonPath(
						"$['paths']['/api/v1/portabilidade/solicitar'].post.responses['422'].content['application/problem+json']")
						.exists())
				.andExpect(jsonPath(
						"$['paths']['/api/v1/portabilidade/operacao/{id}'].get.responses['404'].content['application/problem+json']")
						.exists())
				.andExpect(jsonPath(
						"$['paths']['/api/v1/portabilidade/solicitar'].post.responses['502'].content['application/problem+json']")
						.exists());
	}

	@Test
	void deveServirSwaggerUi() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/swagger-ui/index.html"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Swagger UI")));
	}
}

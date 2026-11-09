package com.portabilidade.api.config.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

	@Bean
	OpenAPI portabilidadeOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("API de Portabilidade de Convênios")
						.description("Contratos da API para autorização, agenda e acompanhamento de portabilidades.")
						.version("v1"));
	}
}

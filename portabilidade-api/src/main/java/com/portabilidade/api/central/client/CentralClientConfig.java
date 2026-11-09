package com.portabilidade.api.central.client;

import java.net.http.HttpClient;

import com.portabilidade.api.central.mapper.CentralExceptionMapper;
import com.portabilidade.api.central.properties.CentralProperties;
import com.portabilidade.api.central.properties.CentralRetryProperties;
import com.portabilidade.api.central.security.HmacSigner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ CentralProperties.class, CentralRetryProperties.class })
public class CentralClientConfig {

	@Bean
	RestClient centralRestClient(RestClient.Builder builder, CentralProperties properties) {
		HttpClient httpClient = HttpClient.newBuilder()
				.version(HttpClient.Version.HTTP_1_1)
				.connectTimeout(properties.connectTimeout())
				.build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(properties.readTimeout());

		return builder
				.baseUrl(properties.baseUrl().toString())
				.requestFactory(requestFactory)
				.build();
	}

	@Bean
	CentralExceptionMapper centralExceptionMapper() {
		return new CentralExceptionMapper();
	}

	@Bean
	CentralGetRetry centralGetRetry(CentralRetryProperties properties) {
		return new CentralGetRetry(properties);
	}

	@Bean
	HmacSigner centralHmacSigner(CentralProperties properties) {
		return new HmacSigner(properties.hmacSecret());
	}

	@Bean
	CentralClient centralClient(
			RestClient centralRestClient,
			CentralExceptionMapper exceptionMapper,
			ObjectMapper objectMapper,
			HmacSigner hmacSigner,
			CentralGetRetry centralGetRetry) {
		return new CentralClient(centralRestClient, exceptionMapper, objectMapper, hmacSigner, centralGetRetry);
	}
}

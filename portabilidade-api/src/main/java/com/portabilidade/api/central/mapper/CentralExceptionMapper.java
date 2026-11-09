package com.portabilidade.api.central.mapper;

import com.portabilidade.api.central.dto.ErroCentralResponse;
import com.portabilidade.api.central.exception.CentralClientException;
import com.portabilidade.api.central.exception.CentralIndisponivelException;
import com.portabilidade.api.central.exception.ProtocoloNaoEncontradoException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

public class CentralExceptionMapper {

	public RuntimeException map(
			RestClientResponseException response,
			String chamada,
			boolean protocoloNaoEncontrado) {
		int status = response.getStatusCode().value();
		String detalhe = extrairDetalhe(response);

		if (status == 503) {
			return new CentralIndisponivelException(
					mensagem("Central indisponível", chamada, detalhe),
					response);
		}
		if (status == 404 && protocoloNaoEncontrado) {
			return new ProtocoloNaoEncontradoException(
					mensagem("Protocolo não encontrado", chamada, detalhe),
					response);
		}
		return new CentralClientException(
				mensagem("Resposta HTTP " + status + " da Central", chamada, detalhe),
				response);
	}

	private String extrairDetalhe(RestClientResponseException response) {
		int status = response.getStatusCode().value();
		if (status != 404 && status != 503) {
			return null;
		}
		String body = response.getResponseBodyAsString();
		if (body.isBlank()) {
			return null;
		}
		try {
			ErroCentralResponse erro = response.getResponseBodyAs(ErroCentralResponse.class);
			return erro == null ? null : erro.erro();
		} catch (RestClientException exception) {
			throw new CentralClientException("A Central retornou um corpo de erro inválido.", exception);
		}
	}

	private String mensagem(String motivo, String chamada, String detalhe) {
		return detalhe == null
				? motivo + " em " + chamada + "."
				: motivo + " em " + chamada + ": " + detalhe;
	}
}

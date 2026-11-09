package com.portabilidade.api.autorizacao.service;

import java.util.Map;

import com.portabilidade.api.autorizacao.dto.OptInResponse;
import com.portabilidade.api.autorizacao.entity.Autorizacao;
import com.portabilidade.api.autorizacao.repository.AutorizacaoRepository;
import com.portabilidade.api.central.client.CentralClient;
import com.portabilidade.api.central.dto.AutorizacaoResponse;
import com.portabilidade.api.central.exception.CentralClientException;
import org.springframework.stereotype.Service;

@Service
public class OptInService {

	private final AutorizacaoRepository autorizacaoRepository;
	private final CentralClient centralClient;

	public OptInService(AutorizacaoRepository autorizacaoRepository, CentralClient centralClient) {
		this.autorizacaoRepository = autorizacaoRepository;
		this.centralClient = centralClient;
	}

	public OptInResponse registrar(String beneficiarioId) {
		return autorizacaoRepository.findByBeneficiarioId(beneficiarioId)
				.map(this::paraResponse)
				.orElseGet(() -> registrarNaCentral(beneficiarioId));
	}

	private OptInResponse registrarNaCentral(String beneficiarioId) {
		AutorizacaoResponse centralResponse = centralClient.registrarAutorizacao(Map.of());
		if (centralResponse == null || centralResponse.autorizacaoId() == null
				|| centralResponse.autorizacaoId().isBlank()) {
			throw new CentralClientException("A Central não retornou o identificador da autorização.");
		}

		Autorizacao autorizacao = autorizacaoRepository.save(
				new Autorizacao(beneficiarioId, centralResponse.autorizacaoId()));
		return paraResponse(autorizacao);
	}

	private OptInResponse paraResponse(Autorizacao autorizacao) {
		return new OptInResponse(
				autorizacao.getBeneficiarioId(),
				autorizacao.getAutorizacaoId(),
				autorizacao.getCriadoEm());
	}
}

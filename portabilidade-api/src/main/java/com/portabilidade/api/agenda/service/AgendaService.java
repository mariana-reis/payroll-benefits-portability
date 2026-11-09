package com.portabilidade.api.agenda.service;

import java.util.List;

import com.portabilidade.api.agenda.dto.AgendaResponse;
import com.portabilidade.api.agenda.dto.ParcelaResponse;
import com.portabilidade.api.agenda.exception.AutorizacaoObrigatoriaException;
import com.portabilidade.api.autorizacao.repository.AutorizacaoRepository;
import com.portabilidade.api.central.client.CentralClient;
import org.springframework.stereotype.Service;

@Service
public class AgendaService {

	private final AutorizacaoRepository autorizacaoRepository;
	private final CentralClient centralClient;

	public AgendaService(AutorizacaoRepository autorizacaoRepository, CentralClient centralClient) {
		this.autorizacaoRepository = autorizacaoRepository;
		this.centralClient = centralClient;
	}

	public AgendaResponse consultar(String beneficiarioId) {
		if (autorizacaoRepository.findByBeneficiarioId(beneficiarioId).isEmpty()) {
			throw new AutorizacaoObrigatoriaException(beneficiarioId);
		}

		com.portabilidade.api.central.dto.AgendaResponse centralAgenda =
				centralClient.consultarAgenda(beneficiarioId);
		List<ParcelaResponse> parcelas = centralAgenda.parcelas().stream()
				.map(parcela -> new ParcelaResponse(
						parcela.parcelaId(),
						parcela.convenioId(),
						parcela.valor(),
						parcela.dataDesconto(),
						parcela.status()))
				.toList();
		return new AgendaResponse(parcelas);
	}
}

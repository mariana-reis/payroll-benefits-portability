package com.portabilidade.api.operacao.domain;

import com.portabilidade.api.operacao.entity.StatusOperacao;
import org.springframework.stereotype.Component;

@Component
public class MaquinaEstadoOperacao {

	public boolean transicaoPermitida(StatusOperacao origem, StatusOperacao destino) {
		if (origem == null || destino == null) {
			return false;
		}
		return switch (origem) {
			case PENDENTE -> destino == StatusOperacao.CONFIRMADA
					|| destino == StatusOperacao.REJEITADA
					|| destino == StatusOperacao.FALHA_INTEGRACAO;
			case FALHA_INTEGRACAO -> destino == StatusOperacao.PENDENTE;
			case CONFIRMADA, REJEITADA -> false;
		};
	}
}

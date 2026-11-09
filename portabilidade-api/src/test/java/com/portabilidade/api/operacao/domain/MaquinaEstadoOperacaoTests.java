package com.portabilidade.api.operacao.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.portabilidade.api.operacao.entity.StatusOperacao;
import org.junit.jupiter.api.Test;

class MaquinaEstadoOperacaoTests {

	private final MaquinaEstadoOperacao maquina = new MaquinaEstadoOperacao();

	@Test
	void permiteTransicoesDefinidasPeloAdr03() {
		assertTrue(maquina.transicaoPermitida(StatusOperacao.PENDENTE, StatusOperacao.CONFIRMADA));
		assertTrue(maquina.transicaoPermitida(StatusOperacao.PENDENTE, StatusOperacao.REJEITADA));
		assertTrue(maquina.transicaoPermitida(StatusOperacao.PENDENTE, StatusOperacao.FALHA_INTEGRACAO));
		assertTrue(maquina.transicaoPermitida(StatusOperacao.FALHA_INTEGRACAO, StatusOperacao.PENDENTE));
	}

	@Test
	void rejeitaTransicoesInvalidasEEstadosTerminais() {
		assertFalse(maquina.transicaoPermitida(StatusOperacao.CONFIRMADA, StatusOperacao.REJEITADA));
		assertFalse(maquina.transicaoPermitida(StatusOperacao.REJEITADA, StatusOperacao.CONFIRMADA));
		assertFalse(maquina.transicaoPermitida(StatusOperacao.FALHA_INTEGRACAO, StatusOperacao.CONFIRMADA));
		assertFalse(maquina.transicaoPermitida(StatusOperacao.PENDENTE, StatusOperacao.PENDENTE));
	}
}

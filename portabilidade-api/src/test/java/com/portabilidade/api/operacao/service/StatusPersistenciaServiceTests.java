package com.portabilidade.api.operacao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import com.portabilidade.api.autorizacao.entity.Autorizacao;
import com.portabilidade.api.operacao.domain.MaquinaEstadoOperacao;
import com.portabilidade.api.operacao.entity.OperacaoPortabilidade;
import com.portabilidade.api.operacao.entity.StatusOperacao;
import com.portabilidade.api.operacao.event.OperacaoEventoService;
import com.portabilidade.api.operacao.repository.OperacaoPortabilidadeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatusPersistenciaServiceTests {

	@Mock
	private OperacaoPortabilidadeRepository operacaoRepository;

	@Mock
	private OperacaoEventoService eventoService;

	private StatusPersistenciaService persistenciaService;

	@BeforeEach
	void configurarServico() {
		persistenciaService = new StatusPersistenciaService(
				operacaoRepository, new MaquinaEstadoOperacao(), eventoService);
	}

	@Test
	void deveIgnorarTransicaoInvalidaSemSalvar() {
		OperacaoPortabilidade atual = operacao(StatusOperacao.CONFIRMADA);
		when(operacaoRepository.findById(atual.getId())).thenReturn(Optional.of(atual));

		OperacaoPortabilidade resultado = persistenciaService.aplicarTransicao(
				atual.getId(), StatusOperacao.REJEITADA, "motivo divergente");

		assertEquals(StatusOperacao.CONFIRMADA, resultado.getStatus());
		verify(operacaoRepository, never()).saveAndFlush(atual);
	}

	@Test
	void devePersistirTransicaoPermitidaESeuMotivo() {
		OperacaoPortabilidade atual = operacao(StatusOperacao.PENDENTE);
		when(operacaoRepository.findById(atual.getId())).thenReturn(Optional.of(atual));
		when(operacaoRepository.saveAndFlush(atual)).thenReturn(atual);

		persistenciaService.aplicarTransicao(atual.getId(), StatusOperacao.REJEITADA, "Margem insuficiente");

		assertEquals(StatusOperacao.REJEITADA, atual.getStatus());
		assertEquals("Margem insuficiente", atual.getMotivoRejeicao());
		verify(operacaoRepository).saveAndFlush(atual);
	}

	@Test
	void devePersistirFalhaDeIntegracaoERegistrarTransicao() {
		OperacaoPortabilidade atual = operacao(StatusOperacao.PENDENTE);
		when(operacaoRepository.findById(atual.getId())).thenReturn(Optional.of(atual));
		when(operacaoRepository.saveAndFlush(atual)).thenReturn(atual);

		persistenciaService.marcarFalhaIntegracao(atual.getId(), "Falha na comunicação com a Central.");

		assertEquals(StatusOperacao.FALHA_INTEGRACAO, atual.getStatus());
		verify(eventoService).registrarFalhaIntegracao(
				atual, StatusOperacao.PENDENTE, "Falha na comunicação com a Central.");
	}

	@Test
	void deveRetornarFalhaParaPendenteERegistrarRetentativa() {
		OperacaoPortabilidade atual = operacao(StatusOperacao.FALHA_INTEGRACAO);
		when(operacaoRepository.findById(atual.getId())).thenReturn(Optional.of(atual));
		when(operacaoRepository.saveAndFlush(atual)).thenReturn(atual);

		var resultado = persistenciaService.prepararReprocessamento(atual.getId());

		assertEquals(StatusOperacao.PENDENTE, resultado.orElseThrow().getStatus());
		verify(eventoService).registrarRetentativa(atual, StatusOperacao.FALHA_INTEGRACAO);
	}

	private OperacaoPortabilidade operacao(StatusOperacao status) {
		OperacaoPortabilidade operacao = new OperacaoPortabilidade(
				new Autorizacao("BEN001", "auth-001"),
				"key-1",
				"hash-1",
				new BigDecimal("300.00"),
				"conta-1",
				status);
		operacao.setProtocoloCentral("protocolo-1");
		return operacao;
	}
}

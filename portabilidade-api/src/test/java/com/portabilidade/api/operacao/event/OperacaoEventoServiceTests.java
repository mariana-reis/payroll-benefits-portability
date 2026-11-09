package com.portabilidade.api.operacao.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;

import com.portabilidade.api.autorizacao.entity.Autorizacao;
import com.portabilidade.api.operacao.entity.OperacaoEventoHistorico;
import com.portabilidade.api.operacao.entity.OperacaoPortabilidade;
import com.portabilidade.api.operacao.entity.StatusOperacao;
import com.portabilidade.api.operacao.entity.TipoEventoOperacao;
import com.portabilidade.api.operacao.repository.OperacaoEventoHistoricoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OperacaoEventoServiceTests {

	@Mock
	private OperacaoEventoHistoricoRepository eventoRepository;

	private OperacaoEventoService eventoService;

	@BeforeEach
	void configurarService() {
		eventoService = new OperacaoEventoService(eventoRepository);
	}

	@Test
	void deveRegistrarEventoCriacao() {
		OperacaoPortabilidade operacao = operacao(StatusOperacao.PENDENTE);

		eventoService.registrarCriacao(operacao);

		OperacaoEventoHistorico evento = eventoCapturado();
		assertEquals(TipoEventoOperacao.OPERACAO_CRIADA, evento.getTipoEvento());
		assertNull(evento.getStatusAnterior());
		assertEquals(StatusOperacao.PENDENTE, evento.getStatusNovo());
	}

	@Test
	void deveRegistrarEventoConfirmacao() {
		OperacaoPortabilidade operacao = operacao(StatusOperacao.CONFIRMADA);

		eventoService.registrarConfirmacao(operacao, StatusOperacao.PENDENTE);

		OperacaoEventoHistorico evento = eventoCapturado();
		assertEquals(TipoEventoOperacao.OPERACAO_CONFIRMADA, evento.getTipoEvento());
		assertEquals(StatusOperacao.PENDENTE, evento.getStatusAnterior());
		assertEquals(StatusOperacao.CONFIRMADA, evento.getStatusNovo());
	}

	@Test
	void deveRegistrarEventoRejeicao() {
		OperacaoPortabilidade operacao = operacao(StatusOperacao.REJEITADA);

		eventoService.registrarRejeicao(operacao, StatusOperacao.PENDENTE);

		OperacaoEventoHistorico evento = eventoCapturado();
		assertEquals(TipoEventoOperacao.OPERACAO_REJEITADA, evento.getTipoEvento());
		assertEquals(StatusOperacao.PENDENTE, evento.getStatusAnterior());
		assertEquals(StatusOperacao.REJEITADA, evento.getStatusNovo());
	}

	@Test
	void deveRegistrarEventoFalha() {
		OperacaoPortabilidade operacao = operacao(StatusOperacao.PENDENTE);

		eventoService.registrarFalhaIntegracao(operacao, "Central indisponível");

		OperacaoEventoHistorico evento = eventoCapturado();
		assertEquals(TipoEventoOperacao.OPERACAO_FALHA_INTEGRACAO, evento.getTipoEvento());
		assertEquals(StatusOperacao.PENDENTE, evento.getStatusAnterior());
		assertEquals(StatusOperacao.PENDENTE, evento.getStatusNovo());
		assertEquals("Central indisponível", evento.getDetalhe());
	}

	@Test
	void deveRegistrarEventoRetentativa() {
		OperacaoPortabilidade operacao = operacao(StatusOperacao.PENDENTE);

		eventoService.registrarRetentativa(operacao);

		OperacaoEventoHistorico evento = eventoCapturado();
		assertEquals(TipoEventoOperacao.OPERACAO_REPROCESSADA, evento.getTipoEvento());
		assertEquals(StatusOperacao.PENDENTE, evento.getStatusAnterior());
		assertEquals(StatusOperacao.PENDENTE, evento.getStatusNovo());
	}

	private OperacaoEventoHistorico eventoCapturado() {
		ArgumentCaptor<OperacaoEventoHistorico> captor = ArgumentCaptor.forClass(OperacaoEventoHistorico.class);
		verify(eventoRepository).saveAndFlush(captor.capture());
		return captor.getValue();
	}

	private OperacaoPortabilidade operacao(StatusOperacao status) {
		return new OperacaoPortabilidade(
				new Autorizacao("BEN001", "auth-001"),
				"key-1",
				"hash-1",
				new BigDecimal("300.00"),
				"conta-1",
				status);
	}
}

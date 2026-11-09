package com.portabilidade.api.operacao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import com.portabilidade.api.autorizacao.entity.Autorizacao;
import com.portabilidade.api.central.client.CentralClient;
import com.portabilidade.api.central.dto.StatusOperacaoResponse;
import com.portabilidade.api.central.exception.CentralIndisponivelException;
import com.portabilidade.api.operacao.dto.OperacaoResponse;
import com.portabilidade.api.operacao.entity.OperacaoPortabilidade;
import com.portabilidade.api.operacao.entity.StatusOperacao;
import com.portabilidade.api.operacao.event.OperacaoEventoService;
import com.portabilidade.api.operacao.exception.OperacaoNaoEncontradaException;
import com.portabilidade.api.operacao.repository.OperacaoPortabilidadeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatusOperacaoServiceTests {

	@Mock
	private OperacaoPortabilidadeRepository operacaoRepository;

	@Mock
	private StatusPersistenciaService persistenciaService;

	@Mock
	private CentralClient centralClient;

	@Mock
	private OperacaoEventoService eventoService;

	@InjectMocks
	private StatusOperacaoService statusOperacaoService;

	@Test
	void deveRetornarTerminalSemConsultarCentral() {
		OperacaoPortabilidade operacao = operacao(StatusOperacao.CONFIRMADA);
		when(operacaoRepository.findWithAutorizacaoById(operacao.getId())).thenReturn(Optional.of(operacao));

		OperacaoResponse response = statusOperacaoService.consultar(operacao.getId());

		assertEquals(StatusOperacao.CONFIRMADA, response.status());
		assertNull(response.retryAfterSeconds());
		verify(centralClient, never()).consultarStatus("protocolo-1");
	}

	@Test
	void deveAplicarTransicaoConfirmada() {
		OperacaoPortabilidade pendente = operacao(StatusOperacao.PENDENTE);
		OperacaoPortabilidade confirmada = operacao(StatusOperacao.CONFIRMADA);
		when(operacaoRepository.findWithAutorizacaoById(pendente.getId())).thenReturn(Optional.of(pendente));
		when(centralClient.consultarStatus("protocolo-1"))
				.thenReturn(new StatusOperacaoResponse("CONFIRMADA", null, null));
		when(persistenciaService.aplicarTransicao(
				pendente.getId(), StatusOperacao.CONFIRMADA, null)).thenReturn(confirmada);

		OperacaoResponse response = statusOperacaoService.consultar(pendente.getId());

		assertEquals(StatusOperacao.CONFIRMADA, response.status());
		verify(persistenciaService).aplicarTransicao(pendente.getId(), StatusOperacao.CONFIRMADA, null);
	}

	@Test
	void deveAplicarTransicaoRejeitadaComMotivo() {
		OperacaoPortabilidade pendente = operacao(StatusOperacao.PENDENTE);
		OperacaoPortabilidade rejeitada = operacao(StatusOperacao.REJEITADA);
		rejeitada.setMotivoRejeicao("Margem insuficiente");
		when(operacaoRepository.findWithAutorizacaoById(pendente.getId())).thenReturn(Optional.of(pendente));
		when(centralClient.consultarStatus("protocolo-1"))
				.thenReturn(new StatusOperacaoResponse("REJEITADA", "Margem insuficiente", null));
		when(persistenciaService.aplicarTransicao(
				pendente.getId(), StatusOperacao.REJEITADA, "Margem insuficiente")).thenReturn(rejeitada);

		OperacaoResponse response = statusOperacaoService.consultar(pendente.getId());

		assertEquals(StatusOperacao.REJEITADA, response.status());
		assertEquals("Margem insuficiente", response.motivoRejeicao());
	}

	@Test
	void deveRetornarRetryAfterEManterPendente() {
		OperacaoPortabilidade pendente = operacao(StatusOperacao.PENDENTE);
		when(operacaoRepository.findWithAutorizacaoById(pendente.getId())).thenReturn(Optional.of(pendente));
		when(centralClient.consultarStatus("protocolo-1"))
				.thenReturn(new StatusOperacaoResponse(null, null, 2L));

		OperacaoResponse response = statusOperacaoService.consultar(pendente.getId());

		assertEquals(StatusOperacao.PENDENTE, response.status());
		assertEquals(2L, response.retryAfterSeconds());
		verify(persistenciaService, never()).aplicarTransicao(pendente.getId(), null, null);
	}

	@Test
	void devePropagarIndisponibilidadeDaCentral() {
		OperacaoPortabilidade pendente = operacao(StatusOperacao.PENDENTE);
		when(operacaoRepository.findWithAutorizacaoById(pendente.getId())).thenReturn(Optional.of(pendente));
		when(centralClient.consultarStatus("protocolo-1"))
				.thenThrow(new CentralIndisponivelException("Central indisponível", new RuntimeException()));

		assertThrows(CentralIndisponivelException.class,
				() -> statusOperacaoService.consultar(pendente.getId()));
	}

	@Test
	void deveRetornarOperacaoNaoEncontrada() {
		UUID id = UUID.randomUUID();
		when(operacaoRepository.findWithAutorizacaoById(id)).thenReturn(Optional.empty());

		assertThrows(OperacaoNaoEncontradaException.class, () -> statusOperacaoService.consultar(id));
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

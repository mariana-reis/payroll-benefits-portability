package com.portabilidade.api.operacao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.portabilidade.api.agenda.dto.AgendaResponse;
import com.portabilidade.api.agenda.dto.ParcelaResponse;
import com.portabilidade.api.agenda.service.AgendaService;
import com.portabilidade.api.autorizacao.entity.Autorizacao;
import com.portabilidade.api.central.client.CentralClient;
import com.portabilidade.api.central.dto.OperacaoPortabilidadeResponse;
import com.portabilidade.api.central.exception.CentralIndisponivelException;
import com.portabilidade.api.operacao.event.OperacaoEventoService;
import com.portabilidade.api.operacao.dto.SolicitarPortabilidadeRequest;
import com.portabilidade.api.operacao.entity.OperacaoPortabilidade;
import com.portabilidade.api.operacao.entity.StatusOperacao;
import com.portabilidade.api.operacao.exception.SolicitacaoInvalidaException;
import com.portabilidade.api.operacao.hash.RequestHashService;
import com.portabilidade.api.operacao.idempotency.IdempotencyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SolicitacaoServiceTests {

	@Mock
	private RequestHashService requestHashService;

	@Mock
	private IdempotencyService idempotencyService;

	@Mock
	private AgendaService agendaService;

	@Mock
	private OperacaoPersistenciaService persistenciaService;

	@Mock
	private CentralClient centralClient;

	@Mock
	private OperacaoEventoService eventoService;

	@Mock
	private StatusPersistenciaService statusPersistenciaService;

	@InjectMocks
	private SolicitacaoService solicitacaoService;

	@Test
	void deveCalcularTotalCorretamenteComValoresDaAgenda() {
		SolicitarPortabilidadeRequest request = request(List.of("P001", "P002"));
		OperacaoPortabilidade operacao = operacao();
		when(requestHashService.gerar(any())).thenReturn("hash-1");
		when(idempotencyService.buscarExistente("key-1", "hash-1")).thenReturn(Optional.empty());
		when(agendaService.consultar("BEN001")).thenReturn(new AgendaResponse(List.of(
				parcela("P001", "150.00", "ABERTA"),
				parcela("P002", "75.25", "ABERTA"))));
		when(persistenciaService.criar(
				eq("BEN001"), eq("key-1"), eq("hash-1"), any(), eq("conta-123"), any()))
				.thenReturn(operacao);
		when(centralClient.solicitarPortabilidade(any()))
				.thenReturn(new OperacaoPortabilidadeResponse("protocolo-1"));
		when(persistenciaService.atualizarProtocolo(operacao.getId(), "protocolo-1"))
				.thenAnswer(invocation -> {
					operacao.setProtocoloCentral("protocolo-1");
					return operacao;
				});

		solicitacaoService.solicitar("key-1", request);

		ArgumentCaptor<BigDecimal> totalCaptor = ArgumentCaptor.forClass(BigDecimal.class);
		verify(persistenciaService).criar(
				eq("BEN001"),
				eq("key-1"),
				eq("hash-1"),
				totalCaptor.capture(),
				eq("conta-123"),
				any());
		assertEquals(new BigDecimal("225.25"), totalCaptor.getValue());
	}

	@Test
	void deveValidarParcelasContraAgendaAtual() {
		SolicitarPortabilidadeRequest request = request(List.of("P_INEXISTENTE"));
		when(requestHashService.gerar(any())).thenReturn("hash-1");
		when(idempotencyService.buscarExistente("key-1", "hash-1")).thenReturn(Optional.empty());
		when(agendaService.consultar("BEN001"))
				.thenReturn(new AgendaResponse(List.of(parcela("P001", "150.00", "ABERTA"))));

		assertThrows(SolicitacaoInvalidaException.class,
				() -> solicitacaoService.solicitar("key-1", request));

		verify(persistenciaService, never()).criar(any(), any(), any(), any(), any(), any());
		verify(centralClient, never()).solicitarPortabilidade(any());
	}

	@Test
	void devePersistirFalhaDeIntegracaoNoStatusEPropagarExcecao() {
		SolicitarPortabilidadeRequest request = request(List.of("P001"));
		OperacaoPortabilidade operacao = operacao();
		when(requestHashService.gerar(any())).thenReturn("hash-1");
		when(idempotencyService.buscarExistente("key-1", "hash-1")).thenReturn(Optional.empty());
		when(agendaService.consultar("BEN001"))
				.thenReturn(new AgendaResponse(List.of(parcela("P001", "150.00", "ABERTA"))));
		when(persistenciaService.criar(any(), any(), any(), any(), any(), any())).thenReturn(operacao);
		when(centralClient.solicitarPortabilidade(any()))
				.thenThrow(new CentralIndisponivelException(
						"Central indisponível", new IllegalStateException("wiremock indisponível")));

		assertThrows(CentralIndisponivelException.class,
				() -> solicitacaoService.solicitar("key-1", request));

		verify(statusPersistenciaService).marcarFalhaIntegracao(
				eq(operacao.getId()), eq("Falha na comunicação com a Central."));
	}

	@Test
	void devePermitirRetentativaIdempotenteAposFalhaDeIntegracao() {
		SolicitarPortabilidadeRequest request = request(List.of("P001"));
		OperacaoPortabilidade operacao = operacao();
		operacao.setStatus(StatusOperacao.FALHA_INTEGRACAO);
		when(requestHashService.gerar(any())).thenReturn("hash-1");
		when(idempotencyService.buscarExistente("key-1", "hash-1")).thenReturn(Optional.of(operacao));
		when(statusPersistenciaService.prepararReprocessamento(operacao.getId()))
				.thenAnswer(invocation -> {
					operacao.setStatus(StatusOperacao.PENDENTE);
					return Optional.of(operacao);
				});
		when(centralClient.solicitarPortabilidade(any()))
				.thenReturn(new OperacaoPortabilidadeResponse("protocolo-retentativa"));
		when(persistenciaService.atualizarProtocolo(operacao.getId(), "protocolo-retentativa"))
				.thenReturn(operacao);

		solicitacaoService.solicitar("key-1", request);

		verify(statusPersistenciaService).prepararReprocessamento(operacao.getId());
		verify(centralClient).solicitarPortabilidade(any());
		verify(persistenciaService).atualizarProtocolo(operacao.getId(), "protocolo-retentativa");
	}

	private SolicitarPortabilidadeRequest request(List<String> parcelaIds) {
		return new SolicitarPortabilidadeRequest("BEN001", parcelaIds, "conta-123");
	}

	private ParcelaResponse parcela(String parcelaId, String valor, String status) {
		return new ParcelaResponse(
				parcelaId,
				"CONV01",
				new BigDecimal(valor),
				LocalDate.parse("2026-01-10"),
				status);
	}

	private OperacaoPortabilidade operacao() {
		return new OperacaoPortabilidade(
				new Autorizacao("BEN001", "auth-001"),
				"key-1",
				"hash-1",
				new BigDecimal("225.25"),
				"conta-123",
				StatusOperacao.PENDENTE);
	}
}

package com.portabilidade.api.agenda.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.portabilidade.api.agenda.dto.AgendaResponse;
import com.portabilidade.api.agenda.exception.AutorizacaoObrigatoriaException;
import com.portabilidade.api.autorizacao.entity.Autorizacao;
import com.portabilidade.api.autorizacao.repository.AutorizacaoRepository;
import com.portabilidade.api.central.client.CentralClient;
import com.portabilidade.api.central.exception.CentralIndisponivelException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgendaServiceTests {

	@Mock
	private AutorizacaoRepository autorizacaoRepository;

	@Mock
	private CentralClient centralClient;

	@InjectMocks
	private AgendaService agendaService;

	@Test
	void deveRetornarAgendaQuandoAutorizado() {
		when(autorizacaoRepository.findByBeneficiarioId("BEN001"))
				.thenReturn(Optional.of(new Autorizacao("BEN001", "auth-001")));
		when(centralClient.consultarAgenda("BEN001")).thenReturn(new com.portabilidade.api.central.dto.AgendaResponse(
				List.of(
						new com.portabilidade.api.central.dto.ParcelaResponse(
								"P001", "CONV01", new BigDecimal("150.00"), LocalDate.parse("2026-01-10"), "ABERTA"),
						new com.portabilidade.api.central.dto.ParcelaResponse(
								"P002", "CONV01", new BigDecimal("150.00"), LocalDate.parse("2026-02-10"), "ABERTA"))));

		AgendaResponse response = agendaService.consultar("BEN001");

		assertEquals(2, response.parcelas().size());
		assertEquals("P001", response.parcelas().getFirst().parcelaId());
		assertEquals("CONV01", response.parcelas().getFirst().convenioId());
		assertEquals(new BigDecimal("150.00"), response.parcelas().getFirst().valor());
		assertEquals(LocalDate.parse("2026-01-10"), response.parcelas().getFirst().dataDesconto());
		assertEquals("ABERTA", response.parcelas().getFirst().status());
		verify(centralClient).consultarAgenda("BEN001");
	}

	@Test
	void deveLancarErroSemAutorizacao() {
		when(autorizacaoRepository.findByBeneficiarioId("BEN001")).thenReturn(Optional.empty());

		assertThrows(AutorizacaoObrigatoriaException.class, () -> agendaService.consultar("BEN001"));

		verify(centralClient, never()).consultarAgenda("BEN001");
	}

	@Test
	void devePropagarErroDaCentral() {
		when(autorizacaoRepository.findByBeneficiarioId("BEN_ERRO"))
				.thenReturn(Optional.of(new Autorizacao("BEN_ERRO", "auth-erro")));
		when(centralClient.consultarAgenda("BEN_ERRO"))
				.thenThrow(new CentralIndisponivelException("Central indisponível", new RuntimeException()));

		assertThrows(CentralIndisponivelException.class, () -> agendaService.consultar("BEN_ERRO"));
	}
}

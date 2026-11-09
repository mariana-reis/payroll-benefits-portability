package com.portabilidade.api.autorizacao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.portabilidade.api.autorizacao.dto.OptInResponse;
import com.portabilidade.api.autorizacao.entity.Autorizacao;
import com.portabilidade.api.autorizacao.repository.AutorizacaoRepository;
import com.portabilidade.api.central.client.CentralClient;
import com.portabilidade.api.central.dto.AutorizacaoResponse;
import com.portabilidade.api.central.exception.CentralIndisponivelException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OptInServiceTests {

	@Mock
	private AutorizacaoRepository autorizacaoRepository;

	@Mock
	private CentralClient centralClient;

	@InjectMocks
	private OptInService optInService;

	@Test
	void devePersistirNovaAutorizacao() {
		when(autorizacaoRepository.findByBeneficiarioId("BEN001")).thenReturn(Optional.empty());
		when(centralClient.registrarAutorizacao(any())).thenReturn(new AutorizacaoResponse("auth-001"));
		when(autorizacaoRepository.save(any())).thenAnswer(invocation -> {
			Autorizacao autorizacao = invocation.getArgument(0);
			ReflectionTestUtils.setField(autorizacao, "criadoEm", java.time.Instant.now());
			return autorizacao;
		});

		OptInResponse response = optInService.registrar("BEN001");

		assertEquals("BEN001", response.beneficiarioId());
		assertEquals("auth-001", response.autorizacaoId());
		assertNotNull(response.criadoEm());
		verify(autorizacaoRepository).save(any(Autorizacao.class));
	}

	@Test
	void deveRetornarAutorizacaoExistente() {
		Autorizacao existente = new Autorizacao("BEN002", "auth-existente");
		java.time.Instant criadoEm = java.time.Instant.parse("2026-01-01T10:00:00Z");
		ReflectionTestUtils.setField(existente, "criadoEm", criadoEm);
		when(autorizacaoRepository.findByBeneficiarioId("BEN002")).thenReturn(Optional.of(existente));

		OptInResponse response = optInService.registrar("BEN002");

		assertEquals("BEN002", response.beneficiarioId());
		assertEquals("auth-existente", response.autorizacaoId());
		assertEquals(existente.getCriadoEm(), response.criadoEm());
		verify(centralClient, never()).registrarAutorizacao(any());
		verify(autorizacaoRepository, never()).save(any());
	}

	@Test
	void devePropagarErroCentral() {
		when(autorizacaoRepository.findByBeneficiarioId("BEN_ERRO")).thenReturn(Optional.empty());
		when(centralClient.registrarAutorizacao(any()))
				.thenThrow(new CentralIndisponivelException("Central indisponível", new RuntimeException()));

		assertThrows(CentralIndisponivelException.class, () -> optInService.registrar("BEN_ERRO"));

		verify(autorizacaoRepository, never()).save(any());
	}
}

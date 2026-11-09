package com.portabilidade.api.operacao.idempotency;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import com.portabilidade.api.autorizacao.entity.Autorizacao;
import com.portabilidade.api.operacao.entity.OperacaoPortabilidade;
import com.portabilidade.api.operacao.entity.StatusOperacao;
import com.portabilidade.api.operacao.exception.IdempotencyPayloadMismatchException;
import com.portabilidade.api.operacao.repository.OperacaoPortabilidadeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTests {

	@Mock
	private OperacaoPortabilidadeRepository operacaoRepository;

	@InjectMocks
	private IdempotencyService idempotencyService;

	@Test
	void deveRetornarMesmaOperacaoMesmoPayload() {
		OperacaoPortabilidade operacao = operacao("hash-1");
		when(operacaoRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(operacao));

		OperacaoPortabilidade existente = idempotencyService.buscarExistente("key-1", "hash-1").orElseThrow();

		assertSame(operacao, existente);
	}

	@Test
	void deveFalharQuandoHashDiferente() {
		when(operacaoRepository.findByIdempotencyKey("key-1"))
				.thenReturn(Optional.of(operacao("hash-1")));

		assertThrows(
				IdempotencyPayloadMismatchException.class,
				() -> idempotencyService.buscarExistente("key-1", "hash-2"));
	}

	private OperacaoPortabilidade operacao(String hash) {
		return new OperacaoPortabilidade(
				new Autorizacao("BEN001", "auth-001"),
				"key-1",
				hash,
				new BigDecimal("300.00"),
				"conta-123",
				StatusOperacao.PENDENTE);
	}
}

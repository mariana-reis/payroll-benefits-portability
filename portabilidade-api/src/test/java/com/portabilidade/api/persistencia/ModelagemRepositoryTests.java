package com.portabilidade.api.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.UUID;

import com.portabilidade.api.autorizacao.entity.Autorizacao;
import com.portabilidade.api.autorizacao.repository.AutorizacaoRepository;
import com.portabilidade.api.operacao.entity.OperacaoEventoHistorico;
import com.portabilidade.api.operacao.entity.OperacaoParcela;
import com.portabilidade.api.operacao.entity.OperacaoPortabilidade;
import com.portabilidade.api.operacao.entity.StatusOperacao;
import com.portabilidade.api.operacao.entity.TipoEventoOperacao;
import com.portabilidade.api.operacao.repository.OperacaoEventoHistoricoRepository;
import com.portabilidade.api.operacao.repository.OperacaoParcelaRepository;
import com.portabilidade.api.operacao.repository.OperacaoPortabilidadeRepository;
import jakarta.persistence.EntityManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ModelagemRepositoryTests {

	@Autowired
	private AutorizacaoRepository autorizacaoRepository;

	@Autowired
	private OperacaoPortabilidadeRepository operacaoRepository;

	@Autowired
	private OperacaoParcelaRepository parcelaRepository;

	@Autowired
	private OperacaoEventoHistoricoRepository eventoRepository;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private Flyway flyway;

	@Test
	void flywayAplicaMigrationInicial() {
		assertTrue(flyway.info().applied().length > 0);
		assertEquals("2", flyway.info().current().getVersion().getVersion());
	}

	@Test
	void persisteEntidadesRelacionamentosEConsultaNosRepositories() {
		Autorizacao autorizacao = autorizacaoRepository.saveAndFlush(criarAutorizacao("BEN001"));
		OperacaoPortabilidade operacao = criarOperacao(autorizacao, "idem-1");
		operacao.adicionarParcela(new OperacaoParcela("parcela-1", new BigDecimal("125.40")));
		operacao = operacaoRepository.saveAndFlush(operacao);
		UUID operacaoId = operacao.getId();
		eventoRepository.saveAndFlush(new OperacaoEventoHistorico(
				operacao,
				TipoEventoOperacao.OPERACAO_CRIADA,
				null,
				StatusOperacao.PENDENTE,
				"Operacao criada",
				"corr-1"));
		entityManager.clear();

		Autorizacao autorizacaoLida = autorizacaoRepository.findByBeneficiarioId("BEN001").orElseThrow();
		OperacaoPortabilidade operacaoLida = operacaoRepository.findByIdempotencyKey("idem-1").orElseThrow();

		assertNotNull(autorizacaoLida.getId());
		assertNotNull(autorizacaoLida.getCriadoEm());
		assertEquals("BEN001", operacaoLida.getAutorizacao().getBeneficiarioId());
		assertEquals(StatusOperacao.PENDENTE, operacaoLida.getStatus());
		assertEquals(0, new BigDecimal("125.40").compareTo(operacaoLida.getValorTotal()));
		assertNotNull(operacaoLida.getCriadoEm());
		assertNotNull(operacaoLida.getAtualizadoEm());
		assertEquals(0L, operacaoLida.getVersion());
		assertEquals(1, parcelaRepository.findAllByOperacao_IdOrderByParcelaIdAsc(operacaoId).size());
		var eventos = eventoRepository.findAllByOperacao_IdOrderByRegistradoEmAscIdAsc(operacaoId);
		assertEquals(1, eventos.size());
		assertEquals(TipoEventoOperacao.OPERACAO_CRIADA, eventos.getFirst().getTipoEvento());
		assertNotNull(eventos.getFirst().getRegistradoEm());
	}

	@Test
	void rejeitaBeneficiariosDuplicados() {
		autorizacaoRepository.saveAndFlush(criarAutorizacao("BEN001"));
		assertThrows(DataIntegrityViolationException.class,
				() -> autorizacaoRepository.saveAndFlush(criarAutorizacao("BEN001")));
	}

	@Test
	void rejeitaChavesDeIdempotenciaDuplicadas() {
		Autorizacao autorizacao = autorizacaoRepository.saveAndFlush(criarAutorizacao("BEN001"));
		operacaoRepository.saveAndFlush(criarOperacao(autorizacao, "idem-duplicada"));
		assertThrows(DataIntegrityViolationException.class,
				() -> operacaoRepository.saveAndFlush(criarOperacao(autorizacao, "idem-duplicada")));
	}

	@Test
	void incrementaVersionERejeitaAtualizacaoObsoleta() {
		Autorizacao autorizacao = autorizacaoRepository.saveAndFlush(criarAutorizacao("BEN001"));
		OperacaoPortabilidade criada = operacaoRepository.saveAndFlush(criarOperacao(autorizacao, "idem-version"));
		UUID operacaoId = criada.getId();
		entityManager.clear();

		OperacaoPortabilidade obsoleta = operacaoRepository.findById(operacaoId).orElseThrow();
		entityManager.detach(obsoleta);
		OperacaoPortabilidade atual = operacaoRepository.findById(operacaoId).orElseThrow();
		entityManager.detach(atual);

		atual.setStatus(StatusOperacao.CONFIRMADA);
		OperacaoPortabilidade atualizada = operacaoRepository.saveAndFlush(atual);
		assertEquals(1L, atualizada.getVersion());

		obsoleta.setStatus(StatusOperacao.REJEITADA);
		assertThrows(ObjectOptimisticLockingFailureException.class,
				() -> operacaoRepository.saveAndFlush(obsoleta));
	}

	private Autorizacao criarAutorizacao(String beneficiarioId) {
		return new Autorizacao(beneficiarioId, "auth-" + beneficiarioId);
	}

	private OperacaoPortabilidade criarOperacao(Autorizacao autorizacao, String idempotencyKey) {
		return new OperacaoPortabilidade(
				autorizacao,
				idempotencyKey,
				"0".repeat(64),
				new BigDecimal("125.40"),
				"conta-destino",
				StatusOperacao.PENDENTE);
	}
}

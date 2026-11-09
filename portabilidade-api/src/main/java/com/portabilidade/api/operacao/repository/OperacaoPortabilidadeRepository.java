package com.portabilidade.api.operacao.repository;

import java.util.Optional;
import java.util.UUID;

import com.portabilidade.api.operacao.entity.OperacaoPortabilidade;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperacaoPortabilidadeRepository extends JpaRepository<OperacaoPortabilidade, UUID> {

	Optional<OperacaoPortabilidade> findByIdempotencyKey(String idempotencyKey);

	@EntityGraph(attributePaths = "autorizacao")
	Optional<OperacaoPortabilidade> findWithAutorizacaoById(UUID id);
}

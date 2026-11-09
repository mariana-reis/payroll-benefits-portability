package com.portabilidade.api.autorizacao.repository;

import java.util.Optional;
import java.util.UUID;

import com.portabilidade.api.autorizacao.entity.Autorizacao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutorizacaoRepository extends JpaRepository<Autorizacao, UUID> {

	Optional<Autorizacao> findByBeneficiarioId(String beneficiarioId);
}

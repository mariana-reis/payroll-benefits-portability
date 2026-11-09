package com.portabilidade.api.operacao.repository;

import java.util.List;
import java.util.UUID;

import com.portabilidade.api.operacao.entity.OperacaoParcela;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperacaoParcelaRepository extends JpaRepository<OperacaoParcela, UUID> {

	List<OperacaoParcela> findAllByOperacao_IdOrderByParcelaIdAsc(UUID operacaoId);
}

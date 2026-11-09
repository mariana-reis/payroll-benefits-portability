package com.portabilidade.api.operacao.repository;

import java.util.List;
import java.util.UUID;

import com.portabilidade.api.operacao.entity.OperacaoEventoHistorico;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperacaoEventoHistoricoRepository extends JpaRepository<OperacaoEventoHistorico, UUID> {

	List<OperacaoEventoHistorico> findAllByOperacao_IdOrderByRegistradoEmAscIdAsc(UUID operacaoId);
}

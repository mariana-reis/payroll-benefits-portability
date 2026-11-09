package com.portabilidade.api.operacao.event;

import java.util.List;
import java.util.UUID;

import com.portabilidade.api.operacao.dto.EventoResponse;
import com.portabilidade.api.operacao.entity.OperacaoEventoHistorico;
import com.portabilidade.api.operacao.entity.OperacaoPortabilidade;
import com.portabilidade.api.operacao.entity.StatusOperacao;
import com.portabilidade.api.operacao.entity.TipoEventoOperacao;
import com.portabilidade.api.operacao.repository.OperacaoEventoHistoricoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperacaoEventoService {

	private static final Logger LOGGER = LoggerFactory.getLogger(OperacaoEventoService.class);
	private static final String CORRELATION_ID_KEY = "correlationId";

	private final OperacaoEventoHistoricoRepository eventoRepository;

	public OperacaoEventoService(OperacaoEventoHistoricoRepository eventoRepository) {
		this.eventoRepository = eventoRepository;
	}

	@Transactional
	public void registrarCriacao(OperacaoPortabilidade operacao) {
		registrar(operacao, TipoEventoOperacao.OPERACAO_CRIADA, null, operacao.getStatus(),
				"Operação de portabilidade criada.");
	}

	@Transactional
	public void registrarProtocolo(OperacaoPortabilidade operacao) {
		registrar(operacao, TipoEventoOperacao.PROTOCOLO_RECEBIDO, operacao.getStatus(), operacao.getStatus(),
				"Protocolo da Central recebido.");
	}

	@Transactional
	public void registrarConfirmacao(OperacaoPortabilidade operacao, StatusOperacao statusAnterior) {
		registrar(operacao, TipoEventoOperacao.OPERACAO_CONFIRMADA, statusAnterior, operacao.getStatus(),
				"Operação confirmada pela Central.");
	}

	@Transactional
	public void registrarRejeicao(OperacaoPortabilidade operacao, StatusOperacao statusAnterior) {
		registrar(operacao, TipoEventoOperacao.OPERACAO_REJEITADA, statusAnterior, operacao.getStatus(),
				operacao.getMotivoRejeicao() == null
						? "Operação rejeitada pela Central."
						: operacao.getMotivoRejeicao());
	}

	@Transactional
	public void registrarFalhaIntegracao(OperacaoPortabilidade operacao, String detalhe) {
		registrar(operacao, TipoEventoOperacao.OPERACAO_FALHA_INTEGRACAO,
				operacao.getStatus(), operacao.getStatus(), detalhe);
	}

	@Transactional
	public void registrarFalhaIntegracao(
			OperacaoPortabilidade operacao,
			StatusOperacao statusAnterior,
			String detalhe) {
		registrar(operacao, TipoEventoOperacao.OPERACAO_FALHA_INTEGRACAO,
				statusAnterior, operacao.getStatus(), detalhe);
	}

	@Transactional
	public void registrarRetentativa(OperacaoPortabilidade operacao) {
		registrar(operacao, TipoEventoOperacao.OPERACAO_REPROCESSADA,
				operacao.getStatus(), operacao.getStatus(),
				"Solicitação idempotente reconhecida; operação existente reutilizada.");
	}

	@Transactional
	public void registrarRetentativa(OperacaoPortabilidade operacao, StatusOperacao statusAnterior) {
		registrar(operacao, TipoEventoOperacao.OPERACAO_REPROCESSADA,
				statusAnterior, operacao.getStatus(),
				"Retentativa explícita autorizada para operação com falha de integração.");
	}

	@Transactional(readOnly = true)
	public List<EventoResponse> consultar(UUID operacaoId) {
		return eventoRepository.findAllByOperacao_IdOrderByRegistradoEmAscIdAsc(operacaoId).stream()
				.map(evento -> new EventoResponse(
						evento.getId(),
						evento.getTipoEvento(),
						evento.getStatusAnterior(),
						evento.getStatusNovo(),
						evento.getDetalhe(),
						evento.getRegistradoEm()))
				.toList();
	}

	private void registrar(
			OperacaoPortabilidade operacao,
			TipoEventoOperacao tipo,
			StatusOperacao statusAnterior,
			StatusOperacao statusNovo,
			String detalhe) {
		String correlationId = correlationIdAtual();
		eventoRepository.saveAndFlush(new OperacaoEventoHistorico(
				operacao, tipo, statusAnterior, statusNovo, detalhe, correlationId));
		LOGGER.info("Evento de operação registrado correlationId={} operacaoId={} status={} tipoEvento={}",
				correlationId, operacao.getId(), statusNovo, tipo);
	}

	private String correlationIdAtual() {
		return MDC.get(CORRELATION_ID_KEY);
	}
}

package com.portabilidade.api.operacao.service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.portabilidade.api.agenda.dto.AgendaResponse;
import com.portabilidade.api.agenda.dto.ParcelaResponse;
import com.portabilidade.api.agenda.service.AgendaService;
import com.portabilidade.api.central.client.CentralClient;
import com.portabilidade.api.central.dto.OperacaoPortabilidadeResponse;
import com.portabilidade.api.central.exception.CentralClientException;
import com.portabilidade.api.central.exception.CentralIndisponivelException;
import com.portabilidade.api.operacao.dto.SolicitarPortabilidadeRequest;
import com.portabilidade.api.operacao.dto.SolicitacaoResponse;
import com.portabilidade.api.operacao.entity.OperacaoPortabilidade;
import com.portabilidade.api.operacao.entity.StatusOperacao;
import com.portabilidade.api.operacao.event.OperacaoEventoService;
import com.portabilidade.api.operacao.exception.CentralClientOperacaoException;
import com.portabilidade.api.operacao.exception.CentralIndisponivelOperacaoException;
import com.portabilidade.api.operacao.exception.SolicitacaoInvalidaException;
import com.portabilidade.api.operacao.hash.RequestHashService;
import com.portabilidade.api.operacao.idempotency.IdempotencyService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
public class SolicitacaoService {

	private final RequestHashService requestHashService;
	private final IdempotencyService idempotencyService;
	private final AgendaService agendaService;
	private final OperacaoPersistenciaService persistenciaService;
	private final CentralClient centralClient;
	private final OperacaoEventoService eventoService;
	private final StatusPersistenciaService statusPersistenciaService;

	public SolicitacaoService(
			RequestHashService requestHashService,
			IdempotencyService idempotencyService,
			AgendaService agendaService,
			OperacaoPersistenciaService persistenciaService,
			CentralClient centralClient,
			OperacaoEventoService eventoService,
			StatusPersistenciaService statusPersistenciaService) {
		this.requestHashService = requestHashService;
		this.idempotencyService = idempotencyService;
		this.agendaService = agendaService;
		this.persistenciaService = persistenciaService;
		this.centralClient = centralClient;
		this.eventoService = eventoService;
		this.statusPersistenciaService = statusPersistenciaService;
	}

	public SolicitacaoResponse solicitar(String chaveIdempotencia, SolicitarPortabilidadeRequest request) {
		SolicitarPortabilidadeRequest normalizada = normalizar(request);
		String hash = requestHashService.gerar(normalizada);

		var existente = idempotencyService.buscarExistente(chaveIdempotencia, hash);
		if (existente.isPresent()) {
			if (existente.get().getStatus() == StatusOperacao.FALHA_INTEGRACAO) {
				return retentarFalha(existente.get(), chaveIdempotencia, hash, normalizada.beneficiarioId());
			}
			eventoService.registrarRetentativa(existente.get());
			return paraResponse(existente.get());
		}

		AgendaResponse agenda = agendaService.consultar(normalizada.beneficiarioId());
		List<ParcelaResponse> parcelas = validarParcelas(normalizada.parcelaIds(), agenda);
		BigDecimal total = parcelas.stream()
				.map(parcela -> parcela.valor())
				.reduce(BigDecimal.ZERO, (acumulado, valor) -> acumulado.add(valor));

		OperacaoPortabilidade operacao;
		try {
			operacao = persistenciaService.criar(
					normalizada.beneficiarioId(),
					chaveIdempotencia,
					hash,
					total,
					normalizada.contaDestino(),
					parcelas);
		} catch (DataIntegrityViolationException conflitoPersistencia) {
			OperacaoPortabilidade vencedora = idempotencyService.recuperarVencedora(
					chaveIdempotencia,
					hash,
					conflitoPersistencia);
			if (vencedora.getStatus() == StatusOperacao.FALHA_INTEGRACAO) {
				return retentarFalha(vencedora, chaveIdempotencia, hash, normalizada.beneficiarioId());
			}
			eventoService.registrarRetentativa(vencedora);
			return paraResponse(vencedora);
		}

		return integrarNaCentral(operacao, normalizada.beneficiarioId());
	}

	private SolicitacaoResponse retentarFalha(
			OperacaoPortabilidade operacao,
			String chaveIdempotencia,
			String hash,
			String beneficiarioId) {
		try {
			var reprocessamento = statusPersistenciaService.prepararReprocessamento(operacao.getId());
			if (reprocessamento.isPresent()) {
				return integrarNaCentral(reprocessamento.get(), beneficiarioId);
			}
		} catch (ObjectOptimisticLockingFailureException conflitoRetentativa) {
			return reutilizarOperacaoAtual(chaveIdempotencia, hash);
		}
		return reutilizarOperacaoAtual(chaveIdempotencia, hash);
	}

	private SolicitacaoResponse reutilizarOperacaoAtual(String chaveIdempotencia, String hash) {
		OperacaoPortabilidade atual = idempotencyService.buscarExistente(chaveIdempotencia, hash)
				.orElseThrow(() -> new IllegalStateException("A operação idempotente não foi encontrada."));
		eventoService.registrarRetentativa(atual);
		return paraResponse(atual);
	}

	private SolicitacaoResponse integrarNaCentral(OperacaoPortabilidade operacao, String beneficiarioId) {
		OperacaoPortabilidadeResponse respostaCentral;
		try {
			respostaCentral = centralClient.solicitarPortabilidade(
					Map.of("beneficiarioId", beneficiarioId));
			if (respostaCentral == null || respostaCentral.protocolo() == null
					|| respostaCentral.protocolo().isBlank()) {
				throw new CentralClientException("A Central não retornou o protocolo da operação.");
			}
		} catch (CentralIndisponivelException exception) {
			statusPersistenciaService.marcarFalhaIntegracao(
					operacao.getId(), "Falha na comunicação com a Central.");
			throw new CentralIndisponivelOperacaoException(operacao.getId(), exception);
		} catch (CentralClientException exception) {
			statusPersistenciaService.marcarFalhaIntegracao(
					operacao.getId(), "Falha na comunicação com a Central.");
			throw new CentralClientOperacaoException(operacao.getId(), exception);
		}

		OperacaoPortabilidade atualizada = persistenciaService.atualizarProtocolo(
				operacao.getId(),
				respostaCentral.protocolo());
		return paraResponse(atualizada);
	}

	private SolicitarPortabilidadeRequest normalizar(SolicitarPortabilidadeRequest request) {
		if (request == null || request.beneficiarioId() == null || request.contaDestino() == null
				|| request.parcelaIds() == null || request.parcelaIds().isEmpty()) {
			throw new SolicitacaoInvalidaException("Beneficiário, parcelas e conta destino são obrigatórios.");
		}

		String beneficiarioId = request.beneficiarioId().trim();
		String contaDestino = request.contaDestino().trim();
		List<String> parcelaIds = request.parcelaIds().stream()
				.map(parcelaId -> parcelaId == null ? "" : parcelaId.trim())
				.toList();
		if (beneficiarioId.isBlank() || contaDestino.isBlank()
				|| parcelaIds.stream().anyMatch(parcelaId -> parcelaId == null || parcelaId.isBlank())) {
			throw new SolicitacaoInvalidaException("Beneficiário, parcelas e conta destino não podem ficar vazios.");
		}
		if (new HashSet<>(parcelaIds).size() != parcelaIds.size()) {
			throw new SolicitacaoInvalidaException("A lista de parcelas não pode conter identificadores duplicados.");
		}
		return new SolicitarPortabilidadeRequest(beneficiarioId, parcelaIds, contaDestino);
	}

	private List<ParcelaResponse> validarParcelas(List<String> parcelaIds, AgendaResponse agenda) {
		Map<String, ParcelaResponse> parcelasDisponiveis = agenda.parcelas().stream()
				.collect(Collectors.toMap(
						parcela -> Objects.requireNonNull(parcela).parcelaId(),
						parcela -> parcela,
						(primeira, ignorada) -> primeira));
		List<ParcelaResponse> selecionadas = parcelaIds.stream()
				.map(parcelaId -> {
					ParcelaResponse parcela = parcelasDisponiveis.get(parcelaId);
					if (parcela == null) {
						throw new SolicitacaoInvalidaException(
								"A parcela " + parcelaId + " não existe na agenda atual do beneficiário.");
					}
					if (!"ABERTA".equals(parcela.status())) {
						throw new SolicitacaoInvalidaException(
								"A parcela " + parcelaId + " não está aberta para portabilidade.");
					}
					if (parcela.valor() == null || parcela.valor().signum() < 0) {
						throw new SolicitacaoInvalidaException(
								"A Central retornou um valor inválido para a parcela " + parcelaId + ".");
					}
					return parcela;
				})
				.toList();
		if (selecionadas.isEmpty()) {
			throw new SolicitacaoInvalidaException("Selecione ao menos uma parcela aberta.");
		}
		return selecionadas;
	}

	private SolicitacaoResponse paraResponse(OperacaoPortabilidade operacao) {
		return new SolicitacaoResponse(
				operacao.getId(),
				operacao.getProtocoloCentral(),
				operacao.getStatus(),
				operacao.getValorTotal(),
				operacao.getCriadoEm());
	}
}

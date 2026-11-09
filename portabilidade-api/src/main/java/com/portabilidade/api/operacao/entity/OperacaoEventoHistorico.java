package com.portabilidade.api.operacao.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Immutable;
import org.hibernate.type.SqlTypes;

@Entity
@Immutable
@Table(name = "operacao_eventos_historico")
public class OperacaoEventoHistorico {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "tipo_evento", nullable = false, length = 40)
	private TipoEventoOperacao tipoEvento;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "operacao_id", nullable = false)
	private OperacaoPortabilidade operacao;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "status_anterior", length = 32)
	private StatusOperacao statusAnterior;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "status_novo", nullable = false, length = 32)
	private StatusOperacao statusNovo;

	@Column(length = 1000)
	private String detalhe;

	@Column(name = "correlation_id", length = 255)
	private String correlationId;

	@Column(name = "registrado_em", nullable = false)
	private Instant registradoEm;

	protected OperacaoEventoHistorico() {
	}

	public OperacaoEventoHistorico(
			OperacaoPortabilidade operacao,
			TipoEventoOperacao tipoEvento,
			StatusOperacao statusAnterior,
			StatusOperacao statusNovo,
			String detalhe,
			String correlationId) {
		this.tipoEvento = tipoEvento;
		this.operacao = operacao;
		this.statusAnterior = statusAnterior;
		this.statusNovo = statusNovo;
		this.detalhe = detalhe;
		this.correlationId = correlationId;
	}

	@PrePersist
	void definirDataRegistro() {
		if (registradoEm == null) {
			registradoEm = Instant.now();
		}
	}

	public UUID getId() {
		return id;
	}

	public TipoEventoOperacao getTipoEvento() {
		return tipoEvento;
	}

	public OperacaoPortabilidade getOperacao() {
		return operacao;
	}

	public StatusOperacao getStatusAnterior() {
		return statusAnterior;
	}

	public StatusOperacao getStatusNovo() {
		return statusNovo;
	}

	public String getDetalhe() {
		return detalhe;
	}

	public String getCorrelationId() {
		return correlationId;
	}

	public Instant getRegistradoEm() {
		return registradoEm;
	}
}

package com.portabilidade.api.operacao.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.portabilidade.api.autorizacao.entity.Autorizacao;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "operacoes_portabilidade")
public class OperacaoPortabilidade {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
			name = "beneficiario_id",
			referencedColumnName = "beneficiario_id",
			nullable = false)
	private Autorizacao autorizacao;

	@Column(name = "idempotency_key", nullable = false, unique = true, length = 255)
	private String idempotencyKey;

	@Column(name = "request_hash", nullable = false, length = 64)
	private String requestHash;

	@Column(name = "protocolo_central", length = 255)
	private String protocoloCentral;

	@Column(name = "valor_total", nullable = false, precision = 19, scale = 2)
	private BigDecimal valorTotal;

	@Column(name = "conta_destino", nullable = false, length = 255)
	private String contaDestino;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 32)
	private StatusOperacao status;

	@Column(name = "motivo_rejeicao", length = 1000)
	private String motivoRejeicao;

	@Column(name = "criado_em", nullable = false)
	private Instant criadoEm;

	@Column(name = "atualizado_em", nullable = false)
	private Instant atualizadoEm;

	@Version
	@Column(nullable = false)
	private long version;

	@OneToMany(mappedBy = "operacao", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OperacaoParcela> parcelas = new ArrayList<>();

	protected OperacaoPortabilidade() {
	}

	public OperacaoPortabilidade(
			Autorizacao autorizacao,
			String idempotencyKey,
			String requestHash,
			BigDecimal valorTotal,
			String contaDestino,
			StatusOperacao status) {
		this.autorizacao = autorizacao;
		this.idempotencyKey = idempotencyKey;
		this.requestHash = requestHash;
		this.valorTotal = valorTotal;
		this.contaDestino = contaDestino;
		this.status = status;
	}

	@PrePersist
	void definirDatasDeCriacao() {
		Instant now = Instant.now();
		if (criadoEm == null) {
			criadoEm = now;
		}
		if (atualizadoEm == null) {
			atualizadoEm = now;
		}
	}

	@PreUpdate
	void atualizarDataDeAlteracao() {
		atualizadoEm = Instant.now();
	}

	public void adicionarParcela(OperacaoParcela parcela) {
		parcelas.add(parcela);
		parcela.associarOperacao(this);
	}

	public UUID getId() {
		return id;
	}

	public Autorizacao getAutorizacao() {
		return autorizacao;
	}

	public String getIdempotencyKey() {
		return idempotencyKey;
	}

	public String getRequestHash() {
		return requestHash;
	}

	public String getProtocoloCentral() {
		return protocoloCentral;
	}

	public void setProtocoloCentral(String protocoloCentral) {
		this.protocoloCentral = protocoloCentral;
	}

	public BigDecimal getValorTotal() {
		return valorTotal;
	}

	public void setValorTotal(BigDecimal valorTotal) {
		this.valorTotal = valorTotal;
	}

	public String getContaDestino() {
		return contaDestino;
	}

	public void setContaDestino(String contaDestino) {
		this.contaDestino = contaDestino;
	}

	public StatusOperacao getStatus() {
		return status;
	}

	public void setStatus(StatusOperacao status) {
		this.status = status;
	}

	public String getMotivoRejeicao() {
		return motivoRejeicao;
	}

	public void setMotivoRejeicao(String motivoRejeicao) {
		this.motivoRejeicao = motivoRejeicao;
	}

	public Instant getCriadoEm() {
		return criadoEm;
	}

	public Instant getAtualizadoEm() {
		return atualizadoEm;
	}

	public long getVersion() {
		return version;
	}

	public List<OperacaoParcela> getParcelas() {
		return List.copyOf(parcelas);
	}

}

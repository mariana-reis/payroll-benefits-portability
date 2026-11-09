package com.portabilidade.api.operacao.entity;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "operacao_parcelas", uniqueConstraints = {
		@UniqueConstraint(
				name = "uk_operacao_parcelas_operacao_parcela",
				columnNames = { "operacao_id", "parcela_id" })
})
public class OperacaoParcela {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "operacao_id", nullable = false)
	private OperacaoPortabilidade operacao;

	@Column(name = "parcela_id", nullable = false, length = 255)
	private String parcelaId;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal valor;

	protected OperacaoParcela() {
	}

	public OperacaoParcela(String parcelaId, BigDecimal valor) {
		this.parcelaId = parcelaId;
		this.valor = valor;
	}

	void associarOperacao(OperacaoPortabilidade operacao) {
		this.operacao = operacao;
	}

	public UUID getId() {
		return id;
	}

	public OperacaoPortabilidade getOperacao() {
		return operacao;
	}

	public String getParcelaId() {
		return parcelaId;
	}

	public BigDecimal getValor() {
		return valor;
	}
}

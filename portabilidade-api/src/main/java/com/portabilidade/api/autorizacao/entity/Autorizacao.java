package com.portabilidade.api.autorizacao.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.portabilidade.api.operacao.entity.OperacaoPortabilidade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "autorizacoes", uniqueConstraints = {
		@UniqueConstraint(name = "uk_autorizacoes_beneficiario", columnNames = "beneficiario_id")
})
public class Autorizacao {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "beneficiario_id", nullable = false, length = 100)
	private String beneficiarioId;

	@Column(name = "autorizacao_id", nullable = false, length = 255)
	private String autorizacaoId;

	@Column(name = "criado_em", nullable = false)
	private Instant criadoEm;

	@OneToMany(mappedBy = "autorizacao")
	private List<OperacaoPortabilidade> operacoes = new ArrayList<>();

	protected Autorizacao() {
	}

	public Autorizacao(String beneficiarioId, String autorizacaoId) {
		this.beneficiarioId = beneficiarioId;
		this.autorizacaoId = autorizacaoId;
	}

	@PrePersist
	void definirDataCriacao() {
		if (criadoEm == null) {
			criadoEm = Instant.now();
		}
	}

	public UUID getId() {
		return id;
	}

	public String getBeneficiarioId() {
		return beneficiarioId;
	}

	public String getAutorizacaoId() {
		return autorizacaoId;
	}

	public Instant getCriadoEm() {
		return criadoEm;
	}

	public List<OperacaoPortabilidade> getOperacoes() {
		return List.copyOf(operacoes);
	}
}

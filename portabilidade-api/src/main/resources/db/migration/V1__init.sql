CREATE TABLE autorizacoes (
    id UUID NOT NULL PRIMARY KEY,
    beneficiario_id VARCHAR(100) NOT NULL,
    autorizacao_id VARCHAR(255) NOT NULL,
    criado_em TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_autorizacoes_beneficiario UNIQUE (beneficiario_id)
);

CREATE TABLE operacoes_portabilidade (
    id UUID NOT NULL PRIMARY KEY,
    beneficiario_id VARCHAR(100) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    protocolo_central VARCHAR(255),
    valor_total NUMERIC(19, 2) NOT NULL,
    conta_destino VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    motivo_rejeicao VARCHAR(1000),
    criado_em TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT uk_operacoes_portabilidade_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT fk_operacoes_portabilidade_autorizacao
        FOREIGN KEY (beneficiario_id) REFERENCES autorizacoes (beneficiario_id),
    CONSTRAINT ck_operacoes_portabilidade_status
        CHECK (status IN ('PENDENTE', 'CONFIRMADA', 'REJEITADA', 'FALHA_INTEGRACAO'))
);

CREATE TABLE operacao_parcelas (
    id UUID NOT NULL PRIMARY KEY,
    operacao_id UUID NOT NULL,
    parcela_id VARCHAR(255) NOT NULL,
    valor NUMERIC(19, 2) NOT NULL,
    CONSTRAINT uk_operacao_parcelas_operacao_parcela UNIQUE (operacao_id, parcela_id),
    CONSTRAINT fk_operacao_parcelas_operacao
        FOREIGN KEY (operacao_id) REFERENCES operacoes_portabilidade (id)
);

CREATE TABLE operacao_eventos_historico (
    id UUID NOT NULL PRIMARY KEY,
    operacao_id UUID NOT NULL,
    status_anterior VARCHAR(32),
    status_novo VARCHAR(32) NOT NULL,
    detalhe VARCHAR(1000),
    correlation_id VARCHAR(255),
    registrado_em TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_operacao_eventos_historico_operacao
        FOREIGN KEY (operacao_id) REFERENCES operacoes_portabilidade (id),
    CONSTRAINT ck_operacao_eventos_status_anterior
        CHECK (status_anterior IS NULL OR status_anterior IN (
            'PENDENTE', 'CONFIRMADA', 'REJEITADA', 'FALHA_INTEGRACAO'
        )),
    CONSTRAINT ck_operacao_eventos_status_novo
        CHECK (status_novo IN ('PENDENTE', 'CONFIRMADA', 'REJEITADA', 'FALHA_INTEGRACAO'))
);

CREATE INDEX idx_operacao_eventos_historico_operacao_registro
    ON operacao_eventos_historico (operacao_id, registrado_em, id);

ALTER TABLE operacao_eventos_historico
    ADD COLUMN tipo_evento VARCHAR(40) DEFAULT 'OPERACAO_CRIADA' NOT NULL;

ALTER TABLE operacao_eventos_historico
    ADD CONSTRAINT ck_operacao_eventos_tipo
        CHECK (tipo_evento IN (
            'OPERACAO_CRIADA',
            'PROTOCOLO_RECEBIDO',
            'OPERACAO_CONFIRMADA',
            'OPERACAO_REJEITADA',
            'OPERACAO_FALHA_INTEGRACAO',
            'OPERACAO_REPROCESSADA'
        ));

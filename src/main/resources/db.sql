-- Esquema inicial do orquestrador para PostgreSQL.
-- Regras como limite de pedidos por operação e transições de estado devem ser
-- validadas também pela camada de serviço, dentro de transações.

CREATE TABLE operation (
    id UUID PRIMARY KEY,
    descricao VARCHAR(255) NOT NULL,
    capacidade_pedidos INTEGER NOT NULL CHECK (capacidade_pedidos > 0),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    prazo TIMESTAMPTZ NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'CRIADA'
        CHECK (status IN ('CRIADA', 'RECEBENDO_PEDIDOS', 'EM_ANDAMENTO', 'CONCLUIDA', 'CANCELADA'))
);

CREATE TABLE order (
    id UUID PRIMARY KEY,
    numero_pedido VARCHAR(64) NOT NULL UNIQUE,
    data_pedido TIMESTAMPTZ NOT NULL,
    operacao_id UUID NOT NULL REFERENCES operacao (id) ON DELETE RESTRICT,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDENTE'
        CHECK (status IN ('PENDENTE', 'EM_ANDAMENTO', 'CONCLUIDO', 'CANCELADO'))
);

CREATE INDEX idx_order_operation ON pedido (operacao_id);

CREATE TABLE task (
    id UUID PRIMARY KEY,
    numero_tarefa VARCHAR(64) NOT NULL UNIQUE,
    pedido_id UUID NOT NULL REFERENCES pedido (id) ON DELETE RESTRICT,
    fase VARCHAR(24) NOT NULL
        CHECK (fase IN ('SEPARACAO', 'CONFERENCIA', 'EMBALAGEM', 'ENTREGA')),
    status VARCHAR(24) NOT NULL DEFAULT 'PENDENTE'
        CHECK (status IN ('PENDENTE', 'DISPONIVEL', 'EM_EXECUCAO', 'CONCLUIDA', 'CANCELADA')),
    prioridade VARCHAR(24) NOT NULL DEFAULT 'NORMAL'
        CHECK (prioridade IN ('BAIXA', 'NORMAL', 'ALTA', 'URGENTE')),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    prazo TIMESTAMPTZ,
    depende_de_tarefa_id UUID REFERENCES tarefa (id) ON DELETE RESTRICT,

    CONSTRAINT uq_tarefa_pedido_fase UNIQUE (pedido_id, fase),
    CONSTRAINT uq_tarefa_id_pedido UNIQUE (id, pedido_id),
    CONSTRAINT fk_tarefa_dependencia_mesmo_pedido
        FOREIGN KEY (depende_de_tarefa_id, pedido_id)
        REFERENCES tarefa (id, pedido_id) ON DELETE RESTRICT,
    CONSTRAINT ck_tarefa_nao_depende_de_si
        CHECK (depende_de_tarefa_id IS NULL OR depende_de_tarefa_id <> id)
);

CREATE INDEX idx_tarefa_status_prioridade_prazo
    ON tarefa (status, prioridade, prazo);

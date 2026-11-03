CREATE TABLE operation (
    id UUID PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    order_capacity INTEGER NOT NULL CHECK (order_capacity > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deadline TIMESTAMPTZ NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'CREATED'
        CHECK (status IN ('CREATED', 'ACCEPTING_ORDERS', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
);

CREATE TABLE customer_order (
    id UUID PRIMARY KEY,
    order_number VARCHAR(64) NOT NULL UNIQUE,
    order_date TIMESTAMPTZ NOT NULL,
    operation_id UUID NOT NULL REFERENCES operation (id) ON DELETE RESTRICT,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX idx_customer_order_operation ON customer_order (operation_id);

CREATE TABLE task (
    id UUID PRIMARY KEY,
    task_number VARCHAR(64) NOT NULL UNIQUE,
    order_id UUID NOT NULL REFERENCES customer_order (id) ON DELETE RESTRICT,
    phase VARCHAR(24) NOT NULL
        CHECK (phase IN ('PICKING', 'INSPECTION', 'PACKING', 'DELIVERY')),
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'AVAILABLE', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    priority VARCHAR(24) NOT NULL DEFAULT 'NORMAL'
        CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deadline TIMESTAMPTZ,
    depends_on_task_id UUID REFERENCES task (id) ON DELETE RESTRICT,

    CONSTRAINT uq_task_order_phase UNIQUE (order_id, phase),
    CONSTRAINT uq_task_id_order UNIQUE (id, order_id),
    CONSTRAINT fk_task_dependency_same_order
        FOREIGN KEY (depends_on_task_id, order_id)
        REFERENCES task (id, order_id) ON DELETE RESTRICT,
    CONSTRAINT ck_task_not_self_dependent
        CHECK (depends_on_task_id IS NULL OR depends_on_task_id <> id)
);

CREATE INDEX idx_task_status_priority_deadline
    ON task (status, priority, deadline);

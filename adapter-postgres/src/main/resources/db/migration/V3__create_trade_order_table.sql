CREATE TABLE trade_order (
    order_id UUID PRIMARY KEY,
    idempotency_key VARCHAR(200) NOT NULL,
    symbol VARCHAR(12) NOT NULL,
    side VARCHAR(8) NOT NULL,
    quantity BIGINT NOT NULL,
    reference_price NUMERIC(19, 4) NOT NULL,
    order_type VARCHAR(16) NOT NULL,
    prepared_at TIMESTAMP WITH TIME ZONE NOT NULL,
    state VARCHAR(32) NOT NULL,
    broker_order_id VARCHAR(64),
    state_reason TEXT,
    state_occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT trade_order_idempotency_key_unique UNIQUE (idempotency_key),
    CONSTRAINT trade_order_idempotency_key_not_blank CHECK (btrim(idempotency_key) <> ''),
    CONSTRAINT trade_order_symbol_not_blank CHECK (btrim(symbol) <> ''),
    CONSTRAINT trade_order_side CHECK (side IN ('BUY', 'SELL')),
    CONSTRAINT trade_order_quantity_positive CHECK (quantity > 0),
    CONSTRAINT trade_order_reference_price_positive CHECK (reference_price > 0),
    CONSTRAINT trade_order_type CHECK (order_type = 'MARKET'),
    CONSTRAINT trade_order_state CHECK (
        state IN ('PENDING_SUBMISSION', 'SUBMITTED', 'REJECTED', 'SUBMISSION_UNKNOWN')
    ),
    CONSTRAINT trade_order_state_time_order CHECK (state_occurred_at >= prepared_at),
    CONSTRAINT trade_order_state_columns_consistency CHECK (
        (
            state = 'PENDING_SUBMISSION'
            AND broker_order_id IS NULL
            AND state_reason IS NULL
        )
        OR
        (
            state = 'SUBMITTED'
            AND broker_order_id IS NOT NULL
            AND btrim(broker_order_id) <> ''
            AND state_reason IS NULL
        )
        OR
        (
            state IN ('REJECTED', 'SUBMISSION_UNKNOWN')
            AND broker_order_id IS NULL
            AND state_reason IS NOT NULL
            AND btrim(state_reason) <> ''
        )
    )
);

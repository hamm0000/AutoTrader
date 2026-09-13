CREATE TABLE zscore_trade_plan (
    plan_id UUID PRIMARY KEY,
    symbol VARCHAR(12) NOT NULL,
    end_date_exclusive DATE NOT NULL,
    lookback_window INTEGER NOT NULL,
    planned_at TIMESTAMP WITH TIME ZONE NOT NULL,
    current_price NUMERIC(19, 4) NOT NULL,
    z_score DOUBLE PRECISION NOT NULL,
    rolling_mean DOUBLE PRECISION NOT NULL,
    rolling_stddev DOUBLE PRECISION NOT NULL,
    signal VARCHAR(8) NOT NULL,
    reason TEXT NOT NULL,
    proposal_side VARCHAR(8),
    proposed_quantity BIGINT,
    proposed_price NUMERIC(19, 4),
    purchase_budget NUMERIC(19, 4) NOT NULL,
    CONSTRAINT zscore_trade_plan_symbol_date_key UNIQUE (symbol, end_date_exclusive),
    CONSTRAINT zscore_trade_plan_symbol_not_blank CHECK (btrim(symbol) <> ''),
    CONSTRAINT zscore_trade_plan_window_positive CHECK (lookback_window > 0),
    CONSTRAINT zscore_trade_plan_current_price_positive CHECK (current_price > 0),
    CONSTRAINT zscore_trade_plan_purchase_budget_positive CHECK (purchase_budget > 0),
    CONSTRAINT zscore_trade_plan_rolling_stddev_nonnegative CHECK (rolling_stddev >= 0),
    CONSTRAINT zscore_trade_plan_signal CHECK (signal IN ('BUY', 'SELL', 'HOLD')),
    CONSTRAINT zscore_trade_plan_reason_not_blank CHECK (btrim(reason) <> ''),
    CONSTRAINT zscore_trade_plan_side_matches_signal CHECK (
        proposal_side IS NULL OR proposal_side = signal
    ),
    CONSTRAINT zscore_trade_plan_proposal_consistency CHECK (
        (
            proposal_side IS NULL
            AND proposed_quantity IS NULL
            AND proposed_price IS NULL
        )
        OR
        (
            proposal_side = 'BUY'
            AND proposed_quantity > 0
            AND proposed_price > 0
            AND proposed_price * proposed_quantity <= purchase_budget
        )
        OR
        (
            proposal_side = 'SELL'
            AND proposed_quantity > 0
            AND proposed_price IS NULL
        )
    )
);

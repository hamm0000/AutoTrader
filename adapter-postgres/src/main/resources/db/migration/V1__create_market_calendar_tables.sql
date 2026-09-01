CREATE TABLE trading_calendar (
    trade_date DATE PRIMARY KEY,
    is_trading_day BOOLEAN NOT NULL,
    session_open TIME WITHOUT TIME ZONE,
    session_close TIME WITHOUT TIME ZONE,
    note TEXT,
    verified_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT trading_calendar_session_consistency CHECK (
        (
            is_trading_day
            AND session_open IS NOT NULL
            AND session_close IS NOT NULL
            AND session_open < session_close
        )
        OR
        (
            NOT is_trading_day
            AND session_open IS NULL
            AND session_close IS NULL
        )
    ),
    CONSTRAINT trading_calendar_note_not_blank CHECK (
        note IS NULL OR btrim(note) <> ''
    )
);

CREATE TABLE trading_calendar_refresh_metadata (
    coverage_from DATE PRIMARY KEY,
    coverage_through DATE NOT NULL,
    last_successful_refresh_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT trading_calendar_refresh_metadata_period CHECK (
        coverage_from <= coverage_through
    )
);

CREATE TABLE market_session_override (
    trade_date DATE PRIMARY KEY,
    session_open TIME WITHOUT TIME ZONE NOT NULL,
    session_close TIME WITHOUT TIME ZONE NOT NULL,
    note TEXT NOT NULL,
    CONSTRAINT market_session_override_period CHECK (
        session_open < session_close
    ),
    CONSTRAINT market_session_override_note_not_blank CHECK (
        btrim(note) <> ''
    )
);

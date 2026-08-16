package dev.hamm0000.autotrader.trading.marketdata.application.port.out;

import java.util.Objects;

public final class MarketDataQueryException extends RuntimeException {

    private final Reason reason;

    public MarketDataQueryException(Reason reason, String message) {
        super(message);
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
    }

    public MarketDataQueryException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
    }

    public Reason reason() {
        return reason;
    }

    public enum Reason {
        PROVIDER_UNAVAILABLE,
        INVALID_RESPONSE,
        INSUFFICIENT_DATA
    }
}

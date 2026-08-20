package dev.hamm0000.autotrader.trading.marketdata.application.port.out;

import java.util.Objects;

public final class MarketDataQueryException extends RuntimeException {

    private final Reason reason;

    private MarketDataQueryException(Reason reason, String message) {
        super(message);
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
    }

    private MarketDataQueryException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
    }

    public static MarketDataQueryException invalidResponse(String message) {
        return new MarketDataQueryException(Reason.INVALID_RESPONSE, message);
    }

    public static MarketDataQueryException invalidResponse(String message, Throwable cause) {
        return new MarketDataQueryException(Reason.INVALID_RESPONSE, message, cause);
    }

    public static MarketDataQueryException providerUnavailable(String message) {
        return new MarketDataQueryException(Reason.PROVIDER_UNAVAILABLE, message);
    }

    public static MarketDataQueryException providerUnavailable(String message, Throwable cause) {
        return new MarketDataQueryException(Reason.PROVIDER_UNAVAILABLE, message, cause);
    }

    public static MarketDataQueryException insufficientData(String message) {
        return new MarketDataQueryException(Reason.INSUFFICIENT_DATA, message);
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

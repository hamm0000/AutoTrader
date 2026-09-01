package dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.status;

import java.util.Objects;

public final class TradingDayStatusQueryException extends RuntimeException {

    private final Reason reason;

    private TradingDayStatusQueryException(Reason reason, String message) {
        super(message);
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
    }

    private TradingDayStatusQueryException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
    }

    public static TradingDayStatusQueryException providerUnavailable(String message) {
        return new TradingDayStatusQueryException(Reason.PROVIDER_UNAVAILABLE, message);
    }

    public static TradingDayStatusQueryException providerUnavailable(
            String message,
            Throwable cause
    ) {
        return new TradingDayStatusQueryException(
                Reason.PROVIDER_UNAVAILABLE,
                message,
                cause
        );
    }

    public static TradingDayStatusQueryException invalidResponse(String message) {
        return new TradingDayStatusQueryException(Reason.INVALID_RESPONSE, message);
    }

    public static TradingDayStatusQueryException invalidResponse(
            String message,
            Throwable cause
    ) {
        return new TradingDayStatusQueryException(Reason.INVALID_RESPONSE, message, cause);
    }

    public Reason reason() {
        return reason;
    }

    public enum Reason {
        PROVIDER_UNAVAILABLE,
        INVALID_RESPONSE
    }
}

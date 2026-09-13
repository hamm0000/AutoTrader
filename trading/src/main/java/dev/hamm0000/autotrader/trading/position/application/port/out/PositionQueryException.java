package dev.hamm0000.autotrader.trading.position.application.port.out;

import java.util.Objects;

public final class PositionQueryException extends RuntimeException {

    private final Reason reason;

    private PositionQueryException(Reason reason, String message) {
        super(message);
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
    }

    private PositionQueryException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
    }

    public static PositionQueryException providerUnavailable(String message) {
        return new PositionQueryException(Reason.PROVIDER_UNAVAILABLE, message);
    }

    public static PositionQueryException providerUnavailable(String message, Throwable cause) {
        return new PositionQueryException(Reason.PROVIDER_UNAVAILABLE, message, cause);
    }

    public static PositionQueryException invalidResponse(String message) {
        return new PositionQueryException(Reason.INVALID_RESPONSE, message);
    }

    public static PositionQueryException invalidResponse(String message, Throwable cause) {
        return new PositionQueryException(Reason.INVALID_RESPONSE, message, cause);
    }

    public Reason reason() {
        return reason;
    }

    public enum Reason {
        PROVIDER_UNAVAILABLE,
        INVALID_RESPONSE
    }
}

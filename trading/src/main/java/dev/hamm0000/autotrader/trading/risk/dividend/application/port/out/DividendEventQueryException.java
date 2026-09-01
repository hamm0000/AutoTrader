package dev.hamm0000.autotrader.trading.risk.dividend.application.port.out;

import java.util.Objects;

public final class DividendEventQueryException extends RuntimeException {

    private final Reason reason;

    private DividendEventQueryException(Reason reason, String message) {
        super(message);
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
    }

    private DividendEventQueryException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
    }

    public static DividendEventQueryException unsupportedEnvironment(String message) {
        return new DividendEventQueryException(Reason.UNSUPPORTED_ENVIRONMENT, message);
    }

    public static DividendEventQueryException invalidResponse(String message) {
        return new DividendEventQueryException(Reason.INVALID_RESPONSE, message);
    }

    public static DividendEventQueryException invalidResponse(String message, Throwable cause) {
        return new DividendEventQueryException(Reason.INVALID_RESPONSE, message, cause);
    }

    public static DividendEventQueryException providerUnavailable(String message) {
        return new DividendEventQueryException(Reason.PROVIDER_UNAVAILABLE, message);
    }

    public static DividendEventQueryException providerUnavailable(String message, Throwable cause) {
        return new DividendEventQueryException(Reason.PROVIDER_UNAVAILABLE, message, cause);
    }

    public Reason reason() {
        return reason;
    }

    public enum Reason {
        UNSUPPORTED_ENVIRONMENT,
        PROVIDER_UNAVAILABLE,
        INVALID_RESPONSE
    }
}

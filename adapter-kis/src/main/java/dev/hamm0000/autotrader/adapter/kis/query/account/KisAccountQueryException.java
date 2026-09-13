package dev.hamm0000.autotrader.adapter.kis.query.account;

final class KisAccountQueryException extends RuntimeException {

    private final Reason reason;

    private KisAccountQueryException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    private KisAccountQueryException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    static KisAccountQueryException invalidResponse(String message) {
        return new KisAccountQueryException(Reason.INVALID_RESPONSE, message);
    }

    static KisAccountQueryException invalidResponse(String message, Throwable cause) {
        return new KisAccountQueryException(Reason.INVALID_RESPONSE, message, cause);
    }

    static KisAccountQueryException providerUnavailable(String message, Throwable cause) {
        return new KisAccountQueryException(Reason.PROVIDER_UNAVAILABLE, message, cause);
    }

    Reason reason() {
        return reason;
    }

    enum Reason {
        PROVIDER_UNAVAILABLE,
        INVALID_RESPONSE
    }
}

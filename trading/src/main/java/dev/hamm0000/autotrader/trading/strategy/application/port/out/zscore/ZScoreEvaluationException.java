package dev.hamm0000.autotrader.trading.strategy.application.port.out.zscore;

import java.util.Objects;

public final class ZScoreEvaluationException extends RuntimeException {

    private final Reason reason;

    public ZScoreEvaluationException(Reason reason, String message) {
        super(message);
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
    }

    public ZScoreEvaluationException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
    }

    public Reason reason() {
        return reason;
    }

    public enum Reason {
        INVALID_REQUEST,
        SERVER_UNAVAILABLE,
        INVALID_RESPONSE
    }
}

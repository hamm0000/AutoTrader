package dev.hamm0000.autotrader.adapter.kis.auth;

public class KisAuthException extends RuntimeException {

    public KisAuthException(String message) {
        super(message);
    }

    public KisAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}

package dev.hamm0000.autotrader.trading.order.application.port.out;

public final class OrderQueryException extends RuntimeException {

    public OrderQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}

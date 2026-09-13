package dev.hamm0000.autotrader.trading.order.application.port.out;

public final class OrderSaveException extends RuntimeException {

    public OrderSaveException(String message, Throwable cause) {
        super(message, cause);
    }
}

package dev.hamm0000.autotrader.trading.order.application.port.out;

@FunctionalInterface
public interface CheckOpenOrder {

    boolean exists(OpenOrderQuery query);
}

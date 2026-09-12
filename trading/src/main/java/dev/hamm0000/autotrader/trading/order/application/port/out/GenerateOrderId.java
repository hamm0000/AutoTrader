package dev.hamm0000.autotrader.trading.order.application.port.out;

import dev.hamm0000.autotrader.trading.order.domain.OrderId;

@FunctionalInterface
public interface GenerateOrderId {

    OrderId generate();
}

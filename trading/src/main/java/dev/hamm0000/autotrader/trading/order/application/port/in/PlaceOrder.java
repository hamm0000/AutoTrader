package dev.hamm0000.autotrader.trading.order.application.port.in;

import dev.hamm0000.autotrader.trading.order.domain.Order;

@FunctionalInterface
public interface PlaceOrder {

    Order place(OrderPlacementCommand command);
}

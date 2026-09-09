package dev.hamm0000.autotrader.trading.order.application.port.out;

import dev.hamm0000.autotrader.trading.order.domain.Order;

public interface SaveOrder {

    void save(Order order);
}

package dev.hamm0000.autotrader.app.live.order;

import dev.hamm0000.autotrader.trading.order.application.port.out.GenerateOrderId;
import dev.hamm0000.autotrader.trading.order.domain.OrderId;
import java.util.UUID;

final class UuidOrderIdGenerator implements GenerateOrderId {

    @Override
    public OrderId generate() {
        return new OrderId(UUID.randomUUID());
    }
}

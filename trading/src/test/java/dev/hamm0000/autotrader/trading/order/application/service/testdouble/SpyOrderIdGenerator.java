package dev.hamm0000.autotrader.trading.order.application.service.testdouble;

import dev.hamm0000.autotrader.trading.order.application.port.out.GenerateOrderId;
import dev.hamm0000.autotrader.trading.order.domain.OrderId;
import java.util.Objects;

public final class SpyOrderIdGenerator implements GenerateOrderId {

    private final OrderId orderId;
    private int generateCount;

    public SpyOrderIdGenerator(OrderId orderId) {
        this.orderId = Objects.requireNonNull(orderId, "orderId must not be null");
    }

    @Override
    public OrderId generate() {
        generateCount++;
        return orderId;
    }

    public int generateCount() {
        return generateCount;
    }
}

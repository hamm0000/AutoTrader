package dev.hamm0000.autotrader.trading.order.application.port.out;

import dev.hamm0000.autotrader.trading.order.domain.Order;
import dev.hamm0000.autotrader.trading.order.domain.OrderIdempotencyKey;
import java.util.Optional;

public interface FindOrder {

    Optional<Order> findByIdempotencyKey(OrderIdempotencyKey idempotencyKey);
}

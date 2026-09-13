package dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in;

import dev.hamm0000.autotrader.trading.order.domain.Order;
import java.util.Objects;
import java.util.Optional;

public record ZScoreTradeExecution(
        ZScoreTradePlan plan,
        Optional<Order> order
) {

    public ZScoreTradeExecution {
        Objects.requireNonNull(plan, "plan must not be null");
        Objects.requireNonNull(order, "order must not be null");
        if (plan.proposal().isPresent() != order.isPresent()) {
            throw new IllegalArgumentException(
                    "order presence must match trade proposal presence"
            );
        }
    }
}

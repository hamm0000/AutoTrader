package dev.hamm0000.autotrader.trading.strategy.zscore.application.service.testdouble;

import dev.hamm0000.autotrader.trading.order.application.port.in.OrderPlacementCommand;
import dev.hamm0000.autotrader.trading.order.application.port.in.PlaceOrder;
import dev.hamm0000.autotrader.trading.order.domain.Order;
import java.util.Objects;

public final class SpyOrderPlacer implements PlaceOrder {

    private final Order result;
    private RuntimeException failure;
    private OrderPlacementCommand command;
    private int placeCount;

    public SpyOrderPlacer(Order result) {
        this.result = Objects.requireNonNull(result, "result must not be null");
    }

    @Override
    public Order place(OrderPlacementCommand command) {
        placeCount++;
        this.command = command;
        if (failure != null) {
            throw failure;
        }
        return result;
    }

    public void failWith(RuntimeException failure) {
        this.failure = Objects.requireNonNull(failure, "failure must not be null");
    }

    public OrderPlacementCommand command() {
        return command;
    }

    public int placeCount() {
        return placeCount;
    }
}

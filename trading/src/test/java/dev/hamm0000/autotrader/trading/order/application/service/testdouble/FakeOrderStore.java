package dev.hamm0000.autotrader.trading.order.application.service.testdouble;

import dev.hamm0000.autotrader.trading.order.application.port.out.FindOrder;
import dev.hamm0000.autotrader.trading.order.application.port.out.SaveOrder;
import dev.hamm0000.autotrader.trading.order.domain.Order;
import dev.hamm0000.autotrader.trading.order.domain.OrderIdempotencyKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class FakeOrderStore implements FindOrder, SaveOrder {

    private final Map<OrderIdempotencyKey, Order> orders = new LinkedHashMap<>();
    private final List<Order> savedOrders = new ArrayList<>();
    private RuntimeException saveFailure;
    private int failingSaveAttempt = 1;
    private int saveAttemptCount;
    private Order orderVisibleAfterSaveFailure;

    @Override
    public Optional<Order> findByIdempotencyKey(OrderIdempotencyKey idempotencyKey) {
        return Optional.ofNullable(orders.get(idempotencyKey));
    }

    @Override
    public void save(Order order) {
        saveAttemptCount++;
        if (saveFailure != null && saveAttemptCount == failingSaveAttempt) {
            if (orderVisibleAfterSaveFailure != null) {
                orders.put(
                        orderVisibleAfterSaveFailure.idempotencyKey(),
                        orderVisibleAfterSaveFailure
                );
            }
            throw saveFailure;
        }
        orders.put(order.idempotencyKey(), order);
        savedOrders.add(order);
    }

    public void put(Order order) {
        orders.put(order.idempotencyKey(), order);
    }

    public void failSaveWith(RuntimeException failure) {
        this.saveFailure = failure;
        this.failingSaveAttempt = 1;
    }

    public void failOnSaveAttempt(int saveAttempt, RuntimeException failure) {
        if (saveAttempt < 1) {
            throw new IllegalArgumentException("saveAttempt must be greater than zero");
        }
        this.failingSaveAttempt = saveAttempt;
        this.saveFailure = failure;
    }

    public void failSaveAfterConcurrentOrder(
            RuntimeException failure,
            Order concurrentOrder
    ) {
        this.saveFailure = failure;
        this.failingSaveAttempt = 1;
        this.orderVisibleAfterSaveFailure = concurrentOrder;
    }

    public List<Order> savedOrders() {
        return List.copyOf(savedOrders);
    }
}

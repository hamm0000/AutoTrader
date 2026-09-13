package dev.hamm0000.autotrader.trading.order.application.service.testdouble;

import dev.hamm0000.autotrader.trading.order.application.port.out.OrderAcknowledgment;
import dev.hamm0000.autotrader.trading.order.application.port.out.SubmitOrder;
import dev.hamm0000.autotrader.trading.order.domain.Order;
import java.util.Objects;

public final class SpyOrderSubmitter implements SubmitOrder {

    private final FakeOrderStore orderStore;
    private final OrderAcknowledgment acknowledgment;
    private RuntimeException failure;
    private Order submittedOrder;
    private boolean savedBeforeSubmission;
    private int submitCount;

    public SpyOrderSubmitter(
            FakeOrderStore orderStore,
            OrderAcknowledgment acknowledgment
    ) {
        this.orderStore = Objects.requireNonNull(orderStore, "orderStore must not be null");
        this.acknowledgment = Objects.requireNonNull(
                acknowledgment,
                "acknowledgment must not be null"
        );
    }

    @Override
    public OrderAcknowledgment submit(Order order) {
        submitCount++;
        submittedOrder = order;
        savedBeforeSubmission = orderStore
                .findByIdempotencyKey(order.idempotencyKey())
                .filter(order::equals)
                .isPresent();
        if (failure != null) {
            throw failure;
        }
        return acknowledgment;
    }

    public void failWith(RuntimeException failure) {
        this.failure = Objects.requireNonNull(failure, "failure must not be null");
    }

    public Order submittedOrder() {
        return submittedOrder;
    }

    public boolean savedBeforeSubmission() {
        return savedBeforeSubmission;
    }

    public int submitCount() {
        return submitCount;
    }
}

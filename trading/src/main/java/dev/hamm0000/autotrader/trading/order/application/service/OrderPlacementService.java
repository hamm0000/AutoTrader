package dev.hamm0000.autotrader.trading.order.application.service;

import dev.hamm0000.autotrader.trading.order.application.port.in.OrderPlacementCommand;
import dev.hamm0000.autotrader.trading.order.application.port.in.PlaceOrder;
import dev.hamm0000.autotrader.trading.order.application.port.out.FindOrder;
import dev.hamm0000.autotrader.trading.order.application.port.out.GenerateOrderId;
import dev.hamm0000.autotrader.trading.order.application.port.out.OrderAcknowledgment;
import dev.hamm0000.autotrader.trading.order.application.port.out.OrderQueryException;
import dev.hamm0000.autotrader.trading.order.application.port.out.OrderSaveException;
import dev.hamm0000.autotrader.trading.order.application.port.out.OrderSubmissionException;
import dev.hamm0000.autotrader.trading.order.application.port.out.SaveOrder;
import dev.hamm0000.autotrader.trading.order.application.port.out.SubmitOrder;
import dev.hamm0000.autotrader.trading.order.domain.Order;
import dev.hamm0000.autotrader.trading.order.domain.OrderIdempotencyKey;
import java.time.InstantSource;
import java.util.Objects;
import java.util.Optional;

// 주문 제출 UseCase
public final class OrderPlacementService implements PlaceOrder {

    private final FindOrder findOrder;
    private final SaveOrder saveOrder;
    private final SubmitOrder submitOrder;
    private final GenerateOrderId generateOrderId;
    private final InstantSource timeSource;

    public OrderPlacementService(
            FindOrder findOrder,
            SaveOrder saveOrder,
            SubmitOrder submitOrder,
            GenerateOrderId generateOrderId,
            InstantSource timeSource
    ) {
        this.findOrder = Objects.requireNonNull(findOrder, "findOrder must not be null");
        this.saveOrder = Objects.requireNonNull(saveOrder, "saveOrder must not be null");
        this.submitOrder = Objects.requireNonNull(submitOrder, "submitOrder must not be null");
        this.generateOrderId = Objects.requireNonNull(
                generateOrderId,
                "generateOrderId must not be null"
        );
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource must not be null");
    }

    @Override
    public Order place(OrderPlacementCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        Optional<Order> existing = findOrder.findByIdempotencyKey(command.idempotencyKey());
        if (existing.isPresent()) {
            return existing.get();
        }

        Order pending = Order.prepare(
                generateOrderId.generate(),
                command.idempotencyKey(),
                command.proposal(),
                timeSource.instant()
        );
        try {
            saveOrder.save(pending); // 주문 제출 전에 DB 저장
        } catch (OrderSaveException saveFailure) {
            return findConcurrentOrder(command.idempotencyKey(), saveFailure);
        }

        Order resolved = submit(pending);
        saveOrder.save(resolved);
        return resolved;
    }

    private Order submit(Order pending) {
        try {
            OrderAcknowledgment acknowledgment = submitOrder.submit(pending);
            return pending.markSubmitted(
                    acknowledgment.brokerOrderId(),
                    timeSource.instant()
            );
        } catch (OrderSubmissionException submissionFailure) {
            return resolveFailure(pending, submissionFailure);
        }
    }

    private Order resolveFailure(Order pending, OrderSubmissionException failure) {
        String reason = failureReason(failure);
        return switch (failure.reason()) {
            case PROVIDER_UNAVAILABLE, REJECTED -> pending.markRejected(
                    reason,
                    timeSource.instant()
            );
            case RESULT_UNKNOWN -> pending.markSubmissionUnknown(
                    reason,
                    timeSource.instant()
            );
        };
    }

    private Order findConcurrentOrder(
            OrderIdempotencyKey idempotencyKey,
            OrderSaveException saveFailure
    ) {
        try {
            return findOrder.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> saveFailure); // 못 찾으면 예외 그대로
        } catch (OrderQueryException queryFailure) {
            saveFailure.addSuppressed(queryFailure); // 저장 실패를 그대로 던지고, 동시 저장 여부를 확인하던 조회 실패도 진단 정보로 넘김
            throw saveFailure;
        }
    }

    private static String failureReason(OrderSubmissionException failure) {
        String message = failure.getMessage();
        if (message == null || message.isBlank()) {
            return "주문 제출 실패: " + failure.reason();
        }
        return message;
    }
}

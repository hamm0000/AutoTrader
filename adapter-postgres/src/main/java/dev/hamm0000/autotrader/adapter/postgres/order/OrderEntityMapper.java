package dev.hamm0000.autotrader.adapter.postgres.order;

import dev.hamm0000.autotrader.trading.order.domain.Order;
import dev.hamm0000.autotrader.trading.order.domain.OrderId;
import dev.hamm0000.autotrader.trading.order.domain.OrderIdempotencyKey;
import dev.hamm0000.autotrader.trading.order.domain.OrderState;

final class OrderEntityMapper {

    private OrderEntityMapper() {
    }

    static OrderEntity toEntity(Order order) {
        StateColumns state = StateColumns.from(order.state());
        return new OrderEntity(
                order.id().value(),
                order.idempotencyKey().value(),
                order.symbol(),
                order.side(),
                order.quantity(),
                order.referencePrice(),
                order.type(),
                order.preparedAt(),
                state.type(),
                state.brokerOrderId(),
                state.reason(),
                order.state().occurredAt()
        );
    }

    static Order toDomain(OrderEntity entity) {
        return new Order(
                new OrderId(entity.orderId()),
                new OrderIdempotencyKey(entity.idempotencyKey()),
                entity.symbol(),
                entity.side(),
                entity.quantity(),
                entity.referencePrice(),
                entity.orderType(),
                entity.preparedAt(),
                toDomainState(entity)
        );
    }

    private static OrderState toDomainState(OrderEntity entity) {
        return switch (entity.state()) {
            case PENDING_SUBMISSION -> {
                requireAbsent(entity.brokerOrderId(), "brokerOrderId", entity.state());
                requireAbsent(entity.stateReason(), "stateReason", entity.state());
                yield new OrderState.PendingSubmission(entity.stateOccurredAt());
            }
            case SUBMITTED -> {
                requireAbsent(entity.stateReason(), "stateReason", entity.state());
                yield new OrderState.Submitted(
                        requireNotBlank(entity.brokerOrderId(), "brokerOrderId", entity.state()),
                        entity.stateOccurredAt()
                );
            }
            case REJECTED -> {
                requireAbsent(entity.brokerOrderId(), "brokerOrderId", entity.state());
                yield new OrderState.Rejected(
                        requireNotBlank(entity.stateReason(), "stateReason", entity.state()),
                        entity.stateOccurredAt()
                );
            }
            case SUBMISSION_UNKNOWN -> {
                requireAbsent(entity.brokerOrderId(), "brokerOrderId", entity.state());
                yield new OrderState.SubmissionUnknown(
                        requireNotBlank(entity.stateReason(), "stateReason", entity.state()),
                        entity.stateOccurredAt()
                );
            }
        };
    }

    private static String requireNotBlank(
            String value,
            String column,
            OrderStateType state
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    column + " must not be blank when order state is " + state
            );
        }
        return value;
    }

    private static void requireAbsent(String value, String column, OrderStateType state) {
        if (value != null) {
            throw new IllegalArgumentException(
                    column + " must be null when order state is " + state
            );
        }
    }

    private record StateColumns(
            OrderStateType type,
            String brokerOrderId,
            String reason
    ) {

        private static StateColumns from(OrderState state) {
            if (state instanceof OrderState.PendingSubmission) {
                return new StateColumns(OrderStateType.PENDING_SUBMISSION, null, null);
            }
            if (state instanceof OrderState.Submitted submitted) {
                return new StateColumns(
                        OrderStateType.SUBMITTED,
                        submitted.brokerOrderId(),
                        null
                );
            }
            if (state instanceof OrderState.Rejected rejected) {
                return new StateColumns(OrderStateType.REJECTED, null, rejected.reason());
            }
            if (state instanceof OrderState.SubmissionUnknown unknown) {
                return new StateColumns(
                        OrderStateType.SUBMISSION_UNKNOWN,
                        null,
                        unknown.reason()
                );
            }
            throw new IllegalArgumentException("unsupported order state: " + state);
        }
    }
}

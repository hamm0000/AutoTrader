package dev.hamm0000.autotrader.adapter.postgres.order;

import dev.hamm0000.autotrader.trading.order.domain.OrderSide;
import dev.hamm0000.autotrader.trading.order.domain.OrderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "trade_order",
        uniqueConstraints = @UniqueConstraint(
                name = "trade_order_idempotency_key_unique",
                columnNames = "idempotency_key"
        )
)
public class OrderEntity {

    @Id
    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "idempotency_key", nullable = false, length = 200)
    private String idempotencyKey;

    @Column(name = "symbol", nullable = false, length = 12)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false, length = 8)
    private OrderSide side;

    @Column(name = "quantity", nullable = false)
    private long quantity;

    @Column(name = "reference_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal referencePrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 16)
    private OrderType orderType;

    @Column(name = "prepared_at", nullable = false)
    private Instant preparedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 32)
    private OrderStateType state;

    @Column(name = "broker_order_id", length = 64)
    private String brokerOrderId;

    @Column(name = "state_reason")
    private String stateReason;

    @Column(name = "state_occurred_at", nullable = false)
    private Instant stateOccurredAt;

    protected OrderEntity() {
    }

    OrderEntity(
            UUID orderId,
            String idempotencyKey,
            String symbol,
            OrderSide side,
            long quantity,
            BigDecimal referencePrice,
            OrderType orderType,
            Instant preparedAt,
            OrderStateType state,
            String brokerOrderId,
            String stateReason,
            Instant stateOccurredAt
    ) {
        this.orderId = orderId;
        this.idempotencyKey = idempotencyKey;
        this.symbol = symbol;
        this.side = side;
        this.quantity = quantity;
        this.referencePrice = referencePrice;
        this.orderType = orderType;
        this.preparedAt = preparedAt;
        this.state = state;
        this.brokerOrderId = brokerOrderId;
        this.stateReason = stateReason;
        this.stateOccurredAt = stateOccurredAt;
    }

    UUID orderId() {
        return orderId;
    }

    String idempotencyKey() {
        return idempotencyKey;
    }

    String symbol() {
        return symbol;
    }

    OrderSide side() {
        return side;
    }

    long quantity() {
        return quantity;
    }

    BigDecimal referencePrice() {
        return referencePrice;
    }

    OrderType orderType() {
        return orderType;
    }

    Instant preparedAt() {
        return preparedAt;
    }

    OrderStateType state() {
        return state;
    }

    String brokerOrderId() {
        return brokerOrderId;
    }

    String stateReason() {
        return stateReason;
    }

    Instant stateOccurredAt() {
        return stateOccurredAt;
    }
}

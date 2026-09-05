package dev.hamm0000.autotrader.trading.order.domain;

import dev.hamm0000.autotrader.trading.proposal.domain.BuyProposal;
import dev.hamm0000.autotrader.trading.proposal.domain.TradeProposal;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record Order(
        OrderId id,
        String symbol,
        OrderSide side,
        long quantity,
        BigDecimal referencePrice,
        OrderType type,
        Instant preparedAt,
        OrderState state
) {

    public Order {
        Objects.requireNonNull(id, "id must not be null");
        requireNotBlank(symbol, "symbol");
        Objects.requireNonNull(side, "side must not be null");
        Objects.requireNonNull(referencePrice, "referencePrice must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(preparedAt, "preparedAt must not be null");
        Objects.requireNonNull(state, "state must not be null");
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        if (referencePrice.signum() <= 0) {
            throw new IllegalArgumentException("referencePrice must be greater than zero");
        }
        if (state.occurredAt().isBefore(preparedAt)) {
            throw new IllegalArgumentException("state occurredAt must not be before preparedAt");
        }
    }

    public static Order prepare(OrderId id, TradeProposal proposal, Instant preparedAt) {
        Objects.requireNonNull(proposal, "proposal must not be null");
        Objects.requireNonNull(preparedAt, "preparedAt must not be null");

        OrderSide side = proposal instanceof BuyProposal ? OrderSide.BUY : OrderSide.SELL;
        return new Order(
                id,
                proposal.symbol(),
                side,
                proposal.quantity(),
                proposal.referencePrice(),
                OrderType.MARKET,
                preparedAt,
                new OrderState.PendingSubmission(preparedAt)
        );
    }

    public Order markSubmitted(String brokerOrderId, Instant occurredAt) {
        ensureSubmissionCanBeResolved();
        return transition(new OrderState.Submitted(brokerOrderId, occurredAt));
    }

    public Order markRejected(String reason, Instant occurredAt) {
        ensureSubmissionCanBeResolved();
        return transition(new OrderState.Rejected(reason, occurredAt));
    }

    public Order markSubmissionUnknown(String reason, Instant occurredAt) {
        if (!(state instanceof OrderState.PendingSubmission)) {
            throw new IllegalStateException("only pending submission can become unknown");
        }
        return transition(new OrderState.SubmissionUnknown(reason, occurredAt));
    }

    public Instant updatedAt() {
        return state.occurredAt();
    }

    private void ensureSubmissionCanBeResolved() {
        if (!(state instanceof OrderState.PendingSubmission)
                && !(state instanceof OrderState.SubmissionUnknown)) {
            throw new IllegalStateException("submission has already been resolved");
        }
    }

    private Order transition(OrderState nextState) {
        Objects.requireNonNull(nextState, "nextState must not be null");
        if (nextState.occurredAt().isBefore(updatedAt())) {
            throw new IllegalArgumentException("occurredAt must not be before updatedAt");
        }
        return new Order(
                id,
                symbol,
                side,
                quantity,
                referencePrice,
                type,
                preparedAt,
                nextState
        );
    }

    private static String requireNotBlank(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}

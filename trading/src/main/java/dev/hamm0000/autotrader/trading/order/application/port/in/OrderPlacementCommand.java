package dev.hamm0000.autotrader.trading.order.application.port.in;

import dev.hamm0000.autotrader.trading.order.domain.OrderIdempotencyKey;
import dev.hamm0000.autotrader.trading.proposal.domain.TradeProposal;
import java.util.Objects;

public record OrderPlacementCommand(
        OrderIdempotencyKey idempotencyKey,
        TradeProposal proposal
) {

    public OrderPlacementCommand {
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        Objects.requireNonNull(proposal, "proposal must not be null");
    }
}

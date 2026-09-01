package dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in;

import dev.hamm0000.autotrader.trading.proposal.domain.TradeProposal;
import java.util.Objects;
import java.util.Optional;

public record ZScoreTradePlan(
        EvaluatedZScore evaluation,
        Optional<TradeProposal> proposal
) {

    public ZScoreTradePlan {
        Objects.requireNonNull(evaluation, "evaluation must not be null");
        Objects.requireNonNull(proposal, "proposal must not be null");
    }
}

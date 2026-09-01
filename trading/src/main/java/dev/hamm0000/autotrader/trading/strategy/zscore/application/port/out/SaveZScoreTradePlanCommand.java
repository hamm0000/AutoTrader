package dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out;

import dev.hamm0000.autotrader.trading.proposal.domain.BuyProposal;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreTradePlan;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreTradePlanningCommand;
import java.time.Instant;
import java.util.Objects;

public record SaveZScoreTradePlanCommand(
        ZScoreTradePlanningCommand planningCommand,
        ZScoreTradePlan plan,
        Instant plannedAt
) {

    public SaveZScoreTradePlanCommand {
        Objects.requireNonNull(planningCommand, "planningCommand must not be null");
        Objects.requireNonNull(plan, "plan must not be null");
        Objects.requireNonNull(plannedAt, "plannedAt must not be null");

        String expectedSymbol = planningCommand.evaluationCommand().symbol();
        if (!expectedSymbol.equals(plan.evaluation().symbol())) {
            throw new IllegalArgumentException("planned symbol must match evaluation command symbol");
        }
        plan.proposal().ifPresent(proposal -> {
            if (!expectedSymbol.equals(proposal.symbol())) {
                throw new IllegalArgumentException(
                        "proposal symbol must match evaluation command symbol"
                );
            }
            if (proposal instanceof BuyProposal buy
                    && buy.purchaseBudget().compareTo(planningCommand.purchaseBudget()) != 0) {
                throw new IllegalArgumentException(
                        "proposal purchaseBudget must match planning purchaseBudget"
                );
            }
        });
    }
}

package dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in;

import java.math.BigDecimal;
import java.util.Objects;

public record ZScoreTradePlanningCommand(
        ZScoreEvaluationCommand evaluationCommand,
        BigDecimal purchaseBudget
) {

    public ZScoreTradePlanningCommand {
        Objects.requireNonNull(evaluationCommand, "evaluationCommand must not be null");
        Objects.requireNonNull(purchaseBudget, "purchaseBudget must not be null");
        if (purchaseBudget.signum() <= 0) {
            throw new IllegalArgumentException("purchaseBudget must be greater than zero");
        }
    }
}

package dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out;

import dev.hamm0000.autotrader.trading.strategy.zscore.domain.StrategySignal;

import java.util.Objects;

public record ZScoreEvaluationResult(
        StrategySignal signal,
        double zScore,
        double rollingMean,
        double rollingStddev,
        String reason
) {

    public ZScoreEvaluationResult {
        Objects.requireNonNull(signal, "signal must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
    }
}

package dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in;

import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.ZScoreEvaluationResult;

@FunctionalInterface
public interface EvaluateZScore {

    ZScoreEvaluationResult evaluate(ZScoreEvaluationCommand command);
}

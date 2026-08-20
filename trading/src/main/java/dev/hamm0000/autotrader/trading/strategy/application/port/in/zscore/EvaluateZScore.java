package dev.hamm0000.autotrader.trading.strategy.application.port.in.zscore;

import dev.hamm0000.autotrader.trading.strategy.application.port.out.zscore.ZScoreEvaluationResult;

@FunctionalInterface
public interface EvaluateZScore {

    ZScoreEvaluationResult evaluate(ZScoreEvaluationCommand command);
}

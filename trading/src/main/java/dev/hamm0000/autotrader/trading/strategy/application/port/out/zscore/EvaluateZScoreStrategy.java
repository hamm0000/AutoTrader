package dev.hamm0000.autotrader.trading.strategy.application.port.out.zscore;

@FunctionalInterface
public interface EvaluateZScoreStrategy {

    ZScoreEvaluationResult evaluate(ZScoreEvaluationRequest request);
}

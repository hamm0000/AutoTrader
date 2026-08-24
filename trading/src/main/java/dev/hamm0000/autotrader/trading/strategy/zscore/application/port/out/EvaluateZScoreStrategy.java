package dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out;

@FunctionalInterface
public interface EvaluateZScoreStrategy {

    ZScoreEvaluationResult evaluate(ZScoreEvaluationRequest request);
}

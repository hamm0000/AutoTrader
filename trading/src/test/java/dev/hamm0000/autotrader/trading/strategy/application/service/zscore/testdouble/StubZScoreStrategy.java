package dev.hamm0000.autotrader.trading.strategy.application.service.zscore.testdouble;

import dev.hamm0000.autotrader.trading.strategy.application.port.out.zscore.EvaluateZScoreStrategy;
import dev.hamm0000.autotrader.trading.strategy.application.port.out.zscore.ZScoreEvaluationRequest;
import dev.hamm0000.autotrader.trading.strategy.application.port.out.zscore.ZScoreEvaluationResult;

public final class StubZScoreStrategy implements EvaluateZScoreStrategy {

    private final ZScoreEvaluationResult result;
    private RuntimeException failure;
    private ZScoreEvaluationRequest request;
    private int evaluationCount;

    public StubZScoreStrategy(ZScoreEvaluationResult result) {
        this.result = result;
    }

    @Override
    public ZScoreEvaluationResult evaluate(ZScoreEvaluationRequest request) {
        evaluationCount++;
        this.request = request;
        if (failure != null) {
            throw failure;
        }
        return result;
    }

    public void failWith(RuntimeException failure) {
        this.failure = failure;
    }

    public ZScoreEvaluationRequest request() {
        return request;
    }

    public int evaluationCount() {
        return evaluationCount;
    }
}

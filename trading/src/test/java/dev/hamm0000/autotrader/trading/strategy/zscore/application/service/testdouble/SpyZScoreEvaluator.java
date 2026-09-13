package dev.hamm0000.autotrader.trading.strategy.zscore.application.service.testdouble;

import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.EvaluateZScore;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.EvaluatedZScore;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreEvaluationCommand;
import java.util.Objects;

public final class SpyZScoreEvaluator implements EvaluateZScore {

    private final EvaluatedZScore result;
    private RuntimeException failure;
    private ZScoreEvaluationCommand command;
    private int evaluationCount;

    public SpyZScoreEvaluator(EvaluatedZScore result) {
        this.result = Objects.requireNonNull(result, "result must not be null");
    }

    @Override
    public EvaluatedZScore evaluate(ZScoreEvaluationCommand command) {
        evaluationCount++;
        this.command = command;
        if (failure != null) {
            throw failure;
        }
        return result;
    }

    public void failWith(RuntimeException failure) {
        this.failure = failure;
    }

    public ZScoreEvaluationCommand command() {
        return command;
    }

    public int evaluationCount() {
        return evaluationCount;
    }
}

package dev.hamm0000.autotrader.trading.strategy.zscore.application.service.testdouble;

import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.PlanZScoreTrade;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreTradePlan;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreTradePlanningCommand;
import java.util.Objects;

public final class SpyZScoreTradePlanner implements PlanZScoreTrade {

    private final ZScoreTradePlan result;
    private RuntimeException failure;
    private ZScoreTradePlanningCommand command;
    private int planCount;

    public SpyZScoreTradePlanner(ZScoreTradePlan result) {
        this.result = Objects.requireNonNull(result, "result must not be null");
    }

    @Override
    public ZScoreTradePlan plan(ZScoreTradePlanningCommand command) {
        planCount++;
        this.command = command;
        if (failure != null) {
            throw failure;
        }
        return result;
    }

    public void failWith(RuntimeException failure) {
        this.failure = Objects.requireNonNull(failure, "failure must not be null");
    }

    public ZScoreTradePlanningCommand command() {
        return command;
    }

    public int planCount() {
        return planCount;
    }
}

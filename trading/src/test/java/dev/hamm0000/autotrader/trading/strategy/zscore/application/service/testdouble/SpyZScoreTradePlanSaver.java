package dev.hamm0000.autotrader.trading.strategy.zscore.application.service.testdouble;

import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.SaveZScoreTradePlan;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.SaveZScoreTradePlanCommand;

public final class SpyZScoreTradePlanSaver implements SaveZScoreTradePlan {

    private SaveZScoreTradePlanCommand command;
    private int saveCount;

    @Override
    public void save(SaveZScoreTradePlanCommand command) {
        saveCount++;
        this.command = command;
    }

    public SaveZScoreTradePlanCommand command() {
        return command;
    }

    public int saveCount() {
        return saveCount;
    }
}

package dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in;

@FunctionalInterface
public interface PlanZScoreTrade {

    ZScoreTradePlan plan(ZScoreTradePlanningCommand command);
}

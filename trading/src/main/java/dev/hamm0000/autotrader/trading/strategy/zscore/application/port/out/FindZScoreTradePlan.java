package dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out;

import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreTradePlan;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreTradePlanningCommand;
import java.util.Optional;

@FunctionalInterface
public interface FindZScoreTradePlan {

    Optional<ZScoreTradePlan> findByPlanningCommand(ZScoreTradePlanningCommand command);
}

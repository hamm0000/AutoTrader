package dev.hamm0000.autotrader.app.live.strategy.zscore;

import dev.hamm0000.autotrader.trading.order.application.port.in.PlaceOrder;
import dev.hamm0000.autotrader.trading.position.application.port.out.LoadPosition;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.EvaluateZScore;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ExecuteZScoreTrade;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.PlanZScoreTrade;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.FindZScoreTradePlan;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.SaveZScoreTradePlan;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.service.ZScoreTradeExecutionService;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.service.ZScoreTradePlanningService;
import dev.hamm0000.autotrader.trading.strategy.zscore.domain.ZScoreTradeProposalPolicy;
import java.time.InstantSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("paper-trading")
@Configuration(proxyBeanMethods = false)
class ZScoreTradeConfiguration {

    @Bean
    PlanZScoreTrade planZScoreTrade(
            EvaluateZScore evaluateZScore,
            LoadPosition loadPosition,
            FindZScoreTradePlan findZScoreTradePlan,
            SaveZScoreTradePlan saveZScoreTradePlan,
            InstantSource instantSource
    ) {
        return new ZScoreTradePlanningService(
                evaluateZScore,
                loadPosition,
                new ZScoreTradeProposalPolicy(),
                findZScoreTradePlan,
                saveZScoreTradePlan,
                instantSource
        );
    }

    @Bean
    ExecuteZScoreTrade executeZScoreTrade(
            PlanZScoreTrade planZScoreTrade,
            PlaceOrder placeOrder
    ) {
        return new ZScoreTradeExecutionService(planZScoreTrade, placeOrder);
    }
}

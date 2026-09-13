package dev.hamm0000.autotrader.app.live.strategy.zscore;

import static org.assertj.core.api.Assertions.assertThat;

import dev.hamm0000.autotrader.trading.marketdata.domain.CurrentPrice;
import dev.hamm0000.autotrader.trading.order.application.port.in.PlaceOrder;
import dev.hamm0000.autotrader.trading.position.application.port.out.LoadPosition;
import dev.hamm0000.autotrader.trading.position.domain.Position;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.EvaluateZScore;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.EvaluatedZScore;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ExecuteZScoreTrade;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.PlanZScoreTrade;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.FindZScoreTradePlan;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.SaveZScoreTradePlan;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.ZScoreEvaluationResult;
import dev.hamm0000.autotrader.trading.strategy.zscore.domain.StrategySignal;
import java.math.BigDecimal;
import java.time.InstantSource;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ZScoreTradeConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(EvaluateZScore.class, () -> command -> new EvaluatedZScore(
                    command.symbol(),
                    new CurrentPrice(BigDecimal.ONE),
                    new ZScoreEvaluationResult(
                            StrategySignal.HOLD,
                            0.0,
                            1.0,
                            0.0,
                            "평가 결과"
                    )
            ))
            .withBean(LoadPosition.class, () -> query -> new Position(query.symbol(), 0))
            .withBean(FindZScoreTradePlan.class, () -> command -> Optional.empty())
            .withBean(SaveZScoreTradePlan.class, () -> command -> { })
            .withBean(PlaceOrder.class, () -> command -> null)
            .withBean(InstantSource.class, InstantSource::system)
            .withUserConfiguration(ZScoreTradeConfiguration.class);

    @Test
    void 모의매매_프로필은_ZScore_계획과_실행_유즈케이스를_조립한다() {
        contextRunner
                .withInitializer(context -> context.getEnvironment()
                        .setActiveProfiles("paper-trading"))
                .run(context -> {
                    assertThat(context).hasSingleBean(PlanZScoreTrade.class);
                    assertThat(context).hasSingleBean(ExecuteZScoreTrade.class);
                });
    }

    @Test
    void 기본_프로필은_ZScore_거래_유즈케이스를_조립하지_않는다() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(PlanZScoreTrade.class);
            assertThat(context).doesNotHaveBean(ExecuteZScoreTrade.class);
        });
    }
}

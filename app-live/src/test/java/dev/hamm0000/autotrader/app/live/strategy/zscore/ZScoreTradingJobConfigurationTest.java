package dev.hamm0000.autotrader.app.live.strategy.zscore;

import static org.assertj.core.api.Assertions.assertThat;

import dev.hamm0000.autotrader.trading.marketcalendar.application.port.in.session.CheckTradingSession;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.TradingSessionDecision;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ExecuteZScoreTrade;
import java.math.BigDecimal;
import java.time.InstantSource;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ZScoreTradingJobConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(CheckTradingSession.class, () -> command -> TradingSessionDecision.start())
            .withBean(ExecuteZScoreTrade.class, () -> command -> null)
            .withBean(InstantSource.class, InstantSource::system)
            .withBean(ZScoreTradingJobProperties.class, () -> new ZScoreTradingJobProperties(
                    9,
                    new BigDecimal("10000000"),
                    List.of(new ZScoreTradingJobProperties.Target(
                            "005930",
                            "삼성전자",
                            "반도체"
                    ))
            ))
            .withUserConfiguration(ZScoreTradingJobConfiguration.class);

    @Test
    void 모의매매_프로필은_ZScore_거래_작업_진입점을_조립한다() {
        contextRunner
                .withInitializer(context -> context.getEnvironment()
                        .setActiveProfiles("paper-trading"))
                .run(context -> assertThat(context)
                        .hasSingleBean(ZScoreTradingJobRunner.class));
    }

    @Test
    void 기본_프로필은_ZScore_거래_작업_진입점을_조립하지_않는다() {
        contextRunner.run(context -> assertThat(context)
                .doesNotHaveBean(ZScoreTradingJobRunner.class));
    }
}

package dev.hamm0000.autotrader.app.live.marketcalendar.session;

import static org.assertj.core.api.Assertions.assertThat;

import dev.hamm0000.autotrader.trading.marketcalendar.application.port.in.session.CheckTradingSession;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.calendar.FindTradingCalendar;
import java.time.Duration;
import java.time.InstantSource;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class TradingSessionConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(FindTradingCalendar.class, () -> period -> Optional.empty())
            .withBean(TradingSessionProperties.class, () -> new TradingSessionProperties(
                    Duration.ofDays(10)
            ))
            .withBean(InstantSource.class, InstantSource::system)
            .withUserConfiguration(TradingSessionConfiguration.class);

    @Test
    void 모의매매_프로필은_저장된_캘린더로_거래_세션을_확인한다() {
        contextRunner
                .withInitializer(context -> context.getEnvironment()
                        .setActiveProfiles("paper-trading"))
                .run(context -> assertThat(context).hasSingleBean(CheckTradingSession.class));
    }

    @Test
    void 기본_프로필은_거래_세션_확인을_조립하지_않는다() {
        contextRunner.run(context ->
                assertThat(context).doesNotHaveBean(CheckTradingSession.class));
    }
}

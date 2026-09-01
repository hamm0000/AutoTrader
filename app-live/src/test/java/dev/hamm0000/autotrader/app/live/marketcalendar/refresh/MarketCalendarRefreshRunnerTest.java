package dev.hamm0000.autotrader.app.live.marketcalendar.refresh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.hamm0000.autotrader.app.live.marketcalendar.refresh.testdouble.SpyTradingCalendarRefresher;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.in.refresh.TradingCalendarRefreshCommand;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.CalendarPeriod;
import java.time.Instant;
import java.time.InstantSource;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class MarketCalendarRefreshRunnerTest {

    private static final Instant NOW = Instant.parse("2026-09-02T09:30:00Z");

    @Test
    void 실행일로부터_설정한_기간의_캘린더를_한_번_갱신한다() {
        // given
        SpyTradingCalendarRefresher refresher = new SpyTradingCalendarRefresher();
        MarketCalendarRefreshRunner runner = new MarketCalendarRefreshRunner(
                refresher,
                new MarketCalendarRefreshProperties(20),
                InstantSource.fixed(NOW)
        );

        // when
        runner.run(null);

        // then
        assertThat(refresher.command()).contains(new TradingCalendarRefreshCommand(
                new CalendarPeriod(
                        LocalDate.of(2026, 9, 2),
                        LocalDate.of(2026, 9, 21)
                )
        ));
        assertThat(refresher.refreshCount()).isEqualTo(1);
    }

    @Test
    void 캘린더_갱신에_실패하면_실패를_숨기지_않는다() {
        // given
        IllegalStateException expected = new IllegalStateException("갱신 실패");
        SpyTradingCalendarRefresher refresher = new SpyTradingCalendarRefresher();
        refresher.failWith(expected);
        MarketCalendarRefreshRunner runner = new MarketCalendarRefreshRunner(
                refresher,
                new MarketCalendarRefreshProperties(20),
                InstantSource.fixed(NOW)
        );

        // when & then
        assertThatThrownBy(() -> runner.run(null)).isSameAs(expected);
    }
}

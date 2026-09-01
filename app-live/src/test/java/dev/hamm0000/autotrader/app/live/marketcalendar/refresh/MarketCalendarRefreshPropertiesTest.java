package dev.hamm0000.autotrader.app.live.marketcalendar.refresh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.hamm0000.autotrader.trading.marketcalendar.domain.CalendarPeriod;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class MarketCalendarRefreshPropertiesTest {

    @Test
    void 조회_기간은_시작일부터_설정한_일수만큼_포함한다() {
        // given
        MarketCalendarRefreshProperties properties =
                new MarketCalendarRefreshProperties(20);
        LocalDate startsOn = LocalDate.of(2026, 9, 2);

        // when
        CalendarPeriod period = properties.periodStartingOn(startsOn);

        // then
        assertThat(period).isEqualTo(new CalendarPeriod(
                startsOn,
                LocalDate.of(2026, 9, 21)
        ));
    }

    @Test
    void 조회_기간이_하루보다_짧으면_설정을_거부한다() {
        assertThatThrownBy(() -> new MarketCalendarRefreshProperties(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("coverageDays must be positive");
    }
}

package dev.hamm0000.autotrader.app.live.marketcalendar.refresh;

import dev.hamm0000.autotrader.trading.marketcalendar.domain.CalendarPeriod;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("autotrader.market-calendar.refresh")
record MarketCalendarRefreshProperties(int coverageDays) {

    MarketCalendarRefreshProperties {
        if (coverageDays < 1) {
            throw new IllegalArgumentException("coverageDays must be positive");
        }
    }

    CalendarPeriod periodStartingOn(LocalDate startsOn) {
        Objects.requireNonNull(startsOn, "startsOn must not be null");
        return new CalendarPeriod(startsOn, startsOn.plusDays(coverageDays - 1L));
    }
}

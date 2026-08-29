package dev.hamm0000.autotrader.trading.marketcalendar.application.port.in.refresh;

import dev.hamm0000.autotrader.trading.marketcalendar.domain.CalendarPeriod;
import java.util.Objects;

public record TradingCalendarRefreshCommand(CalendarPeriod period) {

    public TradingCalendarRefreshCommand {
        Objects.requireNonNull(period, "period must not be null");
    }
}

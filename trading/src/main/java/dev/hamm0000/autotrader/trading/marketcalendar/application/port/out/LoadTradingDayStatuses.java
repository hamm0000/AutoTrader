package dev.hamm0000.autotrader.trading.marketcalendar.application.port.out;

import dev.hamm0000.autotrader.trading.marketcalendar.domain.CalendarPeriod;

@FunctionalInterface
public interface LoadTradingDayStatuses {

    TradingDayStatuses load(CalendarPeriod period);
}

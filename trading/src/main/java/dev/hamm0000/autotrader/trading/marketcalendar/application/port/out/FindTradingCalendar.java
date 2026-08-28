package dev.hamm0000.autotrader.trading.marketcalendar.application.port.out;

import dev.hamm0000.autotrader.trading.marketcalendar.domain.CalendarPeriod;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.TradingCalendar;
import java.util.Optional;

@FunctionalInterface
public interface FindTradingCalendar {

    Optional<TradingCalendar> find(CalendarPeriod period);
}

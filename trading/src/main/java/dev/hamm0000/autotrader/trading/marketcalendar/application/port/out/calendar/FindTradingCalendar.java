package dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.calendar;

import dev.hamm0000.autotrader.trading.marketcalendar.domain.CalendarPeriod;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.TradingCalendar;
import java.util.Optional;

@FunctionalInterface
public interface FindTradingCalendar {

    /**
     * DB로부터 요청 범위의 캘린더를 가져옵니다.
     */
    Optional<TradingCalendar> find(CalendarPeriod period);
}

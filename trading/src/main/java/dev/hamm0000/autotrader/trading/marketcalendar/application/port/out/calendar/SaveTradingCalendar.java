package dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.calendar;

import dev.hamm0000.autotrader.trading.marketcalendar.domain.TradingCalendar;

@FunctionalInterface
public interface SaveTradingCalendar {

    /**
     * 캘린더가 포함하는 범위 전체를 원자적으로 저장하며,
     * 범위 밖에 이미 저장된 일정은 보존합니다.
     */
    void save(TradingCalendar calendar);
}

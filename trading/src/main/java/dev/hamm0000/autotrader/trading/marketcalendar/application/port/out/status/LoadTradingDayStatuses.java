package dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.status;

import dev.hamm0000.autotrader.trading.marketcalendar.domain.CalendarPeriod;

@FunctionalInterface
public interface LoadTradingDayStatuses {

    /**
     * 외부 캘린더 공급자로부터 요청 범위의 모든 날짜를 오름차순으로 조회합니다.
     */
    TradingDayStatuses load(CalendarPeriod period);
}

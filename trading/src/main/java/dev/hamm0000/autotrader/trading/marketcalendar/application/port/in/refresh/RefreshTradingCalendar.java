package dev.hamm0000.autotrader.trading.marketcalendar.application.port.in.refresh;

@FunctionalInterface
public interface RefreshTradingCalendar {

    void refresh(TradingCalendarRefreshCommand command);
}

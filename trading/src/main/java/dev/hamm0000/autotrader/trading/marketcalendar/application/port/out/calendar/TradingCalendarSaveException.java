package dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.calendar;

public final class TradingCalendarSaveException extends RuntimeException {

    public TradingCalendarSaveException(String message) {
        super(message);
    }

    public TradingCalendarSaveException(String message, Throwable cause) {
        super(message, cause);
    }
}

package dev.hamm0000.autotrader.trading.marketcalendar.application.port.out;

public final class TradingCalendarQueryException extends RuntimeException {

    public TradingCalendarQueryException(String message) {
        super(message);
    }

    public TradingCalendarQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}

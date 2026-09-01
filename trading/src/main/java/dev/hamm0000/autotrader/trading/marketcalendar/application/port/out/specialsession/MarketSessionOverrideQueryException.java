package dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.specialsession;

public final class MarketSessionOverrideQueryException extends RuntimeException {

    public MarketSessionOverrideQueryException(String message) {
        super(message);
    }

    public MarketSessionOverrideQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}

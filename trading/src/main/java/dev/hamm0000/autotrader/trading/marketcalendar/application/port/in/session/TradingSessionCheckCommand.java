package dev.hamm0000.autotrader.trading.marketcalendar.application.port.in.session;

import java.time.LocalDate;
import java.util.Objects;

public record TradingSessionCheckCommand(LocalDate marketDate) {

    public TradingSessionCheckCommand {
        Objects.requireNonNull(marketDate, "marketDate must not be null");
    }
}

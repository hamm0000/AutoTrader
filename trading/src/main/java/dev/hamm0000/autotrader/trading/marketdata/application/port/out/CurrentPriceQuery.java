package dev.hamm0000.autotrader.trading.marketdata.application.port.out;

import java.util.Objects;

public record CurrentPriceQuery(String symbol) {

    public CurrentPriceQuery {
        Objects.requireNonNull(symbol, "symbol must not be null");
        if (symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank");
        }
    }
}

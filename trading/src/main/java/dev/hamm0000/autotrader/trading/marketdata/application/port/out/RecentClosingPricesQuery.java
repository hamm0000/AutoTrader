package dev.hamm0000.autotrader.trading.marketdata.application.port.out;

import java.time.LocalDate;
import java.util.Objects;

public record RecentClosingPricesQuery(
        String symbol,
        int count,
        LocalDate endDateExclusive
) {

    public RecentClosingPricesQuery {
        Objects.requireNonNull(symbol, "symbol must not be null");
        Objects.requireNonNull(endDateExclusive, "endDateExclusive must not be null");
        if (symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank");
        }
        if (count < 1) {
            throw new IllegalArgumentException("count must be greater than zero");
        }
    }
}

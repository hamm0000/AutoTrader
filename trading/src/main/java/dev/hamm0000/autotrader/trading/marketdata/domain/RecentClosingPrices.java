package dev.hamm0000.autotrader.trading.marketdata.domain;

import java.util.List;
import java.util.Objects;

public record RecentClosingPrices(List<DailyClosingPrice> values) {

    public RecentClosingPrices {
        Objects.requireNonNull(values, "values must not be null");
        values = List.copyOf(values);
        if (values.isEmpty()) {
            throw new IllegalArgumentException("values must not be empty");
        }
        ensureOldestFirst(values);
    }

    private static void ensureOldestFirst(List<DailyClosingPrice> values) {
        for (int index = 1; index < values.size(); index++) {
            DailyClosingPrice previous = values.get(index - 1);
            DailyClosingPrice current = values.get(index);
            if (!previous.tradingDate().isBefore(current.tradingDate())) {
                throw new IllegalArgumentException(
                        "closing prices must be ordered by trading date from oldest to newest"
                );
            }
        }
    }
}

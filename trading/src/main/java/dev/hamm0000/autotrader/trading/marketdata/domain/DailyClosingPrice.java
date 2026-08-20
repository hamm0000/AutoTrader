package dev.hamm0000.autotrader.trading.marketdata.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record DailyClosingPrice(LocalDate tradingDate, BigDecimal price) {

    public DailyClosingPrice {
        Objects.requireNonNull(tradingDate, "tradingDate must not be null");
        Objects.requireNonNull(price, "price must not be null");
    }
}

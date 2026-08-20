package dev.hamm0000.autotrader.trading.marketdata.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record CurrentPrice(BigDecimal value) {

    public CurrentPrice {
        Objects.requireNonNull(value, "value must not be null");
    }
}

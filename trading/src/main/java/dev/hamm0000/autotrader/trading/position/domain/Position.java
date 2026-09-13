package dev.hamm0000.autotrader.trading.position.domain;

import java.util.Objects;

public record Position(String symbol, long quantity) {

    public Position {
        Objects.requireNonNull(symbol, "symbol must not be null");
        if (symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must not be negative");
        }
    }
}

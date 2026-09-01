package dev.hamm0000.autotrader.trading.position.application.port.out;

import java.util.Objects;

public record PositionQuery(String symbol) {

    public PositionQuery {
        Objects.requireNonNull(symbol, "symbol must not be null");
        if (symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank");
        }
    }
}

package dev.hamm0000.autotrader.trading.order.application.port.out;

import dev.hamm0000.autotrader.trading.order.domain.OrderSide;
import java.util.Objects;

public record OpenOrderQuery(String symbol, OrderSide side) {

    public OpenOrderQuery {
        Objects.requireNonNull(symbol, "symbol must not be null");
        Objects.requireNonNull(side, "side must not be null");
        if (symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank");
        }
    }
}

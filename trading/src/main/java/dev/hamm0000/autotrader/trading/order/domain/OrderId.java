package dev.hamm0000.autotrader.trading.order.domain;

import java.util.Objects;
import java.util.UUID;

public record OrderId(UUID value) {

    public OrderId {
        Objects.requireNonNull(value, "value must not be null");
    }
}

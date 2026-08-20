package dev.hamm0000.autotrader.trading.strategy.application.port.in.zscore;

import java.time.LocalDate;
import java.util.Objects;

public record ZScoreEvaluationCommand(
        String symbol,
        int window,
        LocalDate endDateExclusive
) {

    public ZScoreEvaluationCommand {
        Objects.requireNonNull(symbol, "symbol must not be null");
        Objects.requireNonNull(endDateExclusive, "endDateExclusive must not be null");
        if (symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank");
        }
        if (window < 1) {
            throw new IllegalArgumentException("window must be greater than zero");
        }
    }
}

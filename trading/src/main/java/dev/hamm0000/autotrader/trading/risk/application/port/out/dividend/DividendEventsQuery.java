package dev.hamm0000.autotrader.trading.risk.application.port.out.dividend;

import java.time.LocalDate;
import java.util.Objects;


// 시작일은 제외, 종료일은 포함: 시작일은 종가에 반영되기 때문
public record DividendEventsQuery(
        String symbol,
        LocalDate startDateExclusive,
        LocalDate endDateInclusive
) {

    public DividendEventsQuery {
        Objects.requireNonNull(symbol, "symbol must not be null");
        Objects.requireNonNull(startDateExclusive, "startDateExclusive must not be null");
        Objects.requireNonNull(endDateInclusive, "endDateInclusive must not be null");
        if (symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank");
        }
        if (!startDateExclusive.isBefore(endDateInclusive)) {
            throw new IllegalArgumentException("startDateExclusive must be before endDateInclusive");
        }
    }
}

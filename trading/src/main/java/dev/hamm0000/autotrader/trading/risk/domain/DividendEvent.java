package dev.hamm0000.autotrader.trading.risk.domain;

import java.time.LocalDate;
import java.util.Objects;

public record DividendEvent(LocalDate recordDate, LocalDate exDividendDate) {

    public DividendEvent {
        Objects.requireNonNull(recordDate, "recordDate must not be null");
        Objects.requireNonNull(exDividendDate, "exDividendDate must not be null");
        if (!exDividendDate.isBefore(recordDate)) {
            throw new IllegalArgumentException("exDividendDate must be before recordDate");
        }
    }
}

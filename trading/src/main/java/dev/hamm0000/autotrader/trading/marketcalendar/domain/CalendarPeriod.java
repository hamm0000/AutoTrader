package dev.hamm0000.autotrader.trading.marketcalendar.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;


/**
 * 거래 캘린더 조회 범위
 */
public record CalendarPeriod(LocalDate from, LocalDate through) {

    public CalendarPeriod {
        Objects.requireNonNull(from, "from must not be null");
        Objects.requireNonNull(through, "through must not be null");
        if (from.isAfter(through)) {
            throw new IllegalArgumentException("from must not be after through");
        }
    }

    public long dayCount() {
        return ChronoUnit.DAYS.between(from, through) + 1;
    }

    public boolean contains(LocalDate date) {
        Objects.requireNonNull(date, "date must not be null");
        return !date.isBefore(from) && !date.isAfter(through);
    }
}

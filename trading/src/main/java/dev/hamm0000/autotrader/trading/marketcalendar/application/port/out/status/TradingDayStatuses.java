package dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.status;

import dev.hamm0000.autotrader.trading.marketcalendar.domain.CalendarPeriod;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * 조회 범위와 일치하고 날짜 오름차순으로 정렬된 날짜별 개장 여부입니다.
 */
public record TradingDayStatuses(
        CalendarPeriod coverage,
        List<TradingDayStatusObservation> values
) {

    public TradingDayStatuses {
        Objects.requireNonNull(coverage, "coverage must not be null");
        Objects.requireNonNull(values, "values must not be null");
        values = List.copyOf(values);
        validateCoverage(coverage, values);
    }

    private static void validateCoverage(
            CalendarPeriod coverage,
            List<TradingDayStatusObservation> values
    ) {
        if (values.size() != coverage.dayCount()) {
            throw new IllegalArgumentException(
                    "trading day statuses must cover every date in the requested range"
            );
        }

        for (int index = 0; index < values.size(); index++) {
            TradingDayStatusObservation value = values.get(index);
            LocalDate expectedDate = coverage.from().plusDays(index);
            if (!value.date().equals(expectedDate)) {
                throw new IllegalArgumentException(
                        "trading day statuses must be ordered and contain every covered date"
                );
            }
        }
    }
}

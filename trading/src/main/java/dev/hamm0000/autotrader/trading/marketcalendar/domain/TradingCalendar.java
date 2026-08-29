package dev.hamm0000.autotrader.trading.marketcalendar.domain;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;


public final class TradingCalendar {

    private final CalendarPeriod coverage;
    private final List<TradingDay> values;
    private final Map<LocalDate, TradingDay> valuesByDate;

    public TradingCalendar(
            CalendarPeriod coverage,
            List<TradingDay> values
    ) {
        this.coverage = Objects.requireNonNull(coverage, "coverage must not be null");
        Objects.requireNonNull(values, "values must not be null");

        this.values = List.copyOf(values);
        this.valuesByDate = validateAndIndex(this.values);
    }

    public CalendarPeriod coverage() {
        return coverage;
    }

    public List<TradingDay> values() {
        return values;
    }

    public Optional<TradingDay> find(LocalDate date) {
        Objects.requireNonNull(date, "date must not be null");
        return Optional.ofNullable(valuesByDate.get(date));
    }

    /**
     * 배당락일 조회 목적이며,
     * 기준일 직전부터 역순으로 조회 범위 안의 가장 가까운 개장일을 찾습니다.
     * 조회 범위가 부족하면 추측하지 않고 빈 결과를 반환합니다.
     */
    public Optional<LocalDate> findPreviousTradingDay(LocalDate date) {
        Objects.requireNonNull(date, "date must not be null");
        if (!date.isAfter(coverage.from())) {
            return Optional.empty();
        }

        LocalDate candidate = date.minusDays(1);
        if (candidate.isAfter(coverage.through())) {
            return Optional.empty();
        }
        while (!candidate.isBefore(coverage.from())) {
            TradingDay tradingDay = valuesByDate.get(candidate);
            if (tradingDay != null && tradingDay.isTradingDay()) {
                return Optional.of(candidate);
            }
            if (candidate.equals(coverage.from())) {
                break;
            }
            candidate = candidate.minusDays(1);
        }
        return Optional.empty();
    }

    private Map<LocalDate, TradingDay> validateAndIndex(List<TradingDay> tradingDays) {
        if (tradingDays.size() != coverage.dayCount()) {
            throw new IllegalArgumentException(
                    "trading days must cover every date in the requested range"
            );
        }

        Map<LocalDate, TradingDay> indexed = new HashMap<>();
        for (int index = 0; index < tradingDays.size(); index++) {
            TradingDay tradingDay = tradingDays.get(index);
            LocalDate expectedDate = coverage.from().plusDays(index);
            if (!tradingDay.date().equals(expectedDate)) {
                throw new IllegalArgumentException(
                        "trading days must be ordered and contain every covered date"
                );
            }
            indexed.put(tradingDay.date(), tradingDay);
        }
        return Map.copyOf(indexed);
    }
}

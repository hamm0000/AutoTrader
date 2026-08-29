package dev.hamm0000.autotrader.trading.marketcalendar.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 개장 상태와 날짜별 예외를 조합해 하루의 거래 세션을 만듭니다.
 */
public final class MarketSessionSchedule {

    private static final MarketSession REGULAR_SESSION = new MarketSession(
            LocalTime.of(9, 0),
            LocalTime.of(15, 30)
    );

    private final Map<LocalDate, MarketSessionOverride> overridesByDate;

    public MarketSessionSchedule(List<MarketSessionOverride> overrides) {
        Objects.requireNonNull(overrides, "overrides must not be null");
        overridesByDate = indexByDate(List.copyOf(overrides));
    }

    public TradingDay createTradingDay(
            LocalDate date,
            TradingDayStatus status,
            Instant verifiedAt
    ) {
        Objects.requireNonNull(date, "date must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(verifiedAt, "verifiedAt must not be null");

        return switch (status) {
            case CLOSED -> TradingDay.closed(date, verifiedAt);
            case OPEN -> createOpenTradingDay(date, verifiedAt);
        };
    }

    private TradingDay createOpenTradingDay(LocalDate date, Instant verifiedAt) {
        MarketSessionOverride override = overridesByDate.get(date);
        if (override == null) {
            return TradingDay.open(date, REGULAR_SESSION, verifiedAt);
        }
        return TradingDay.open(date, override.session(), override.note(), verifiedAt);
    }

    private static Map<LocalDate, MarketSessionOverride> indexByDate(
            List<MarketSessionOverride> overrides
    ) {
        Map<LocalDate, MarketSessionOverride> indexed = new HashMap<>();
        for (MarketSessionOverride override : overrides) {
            MarketSessionOverride previous = indexed.putIfAbsent(override.date(), override);
            if (previous != null) {
                throw new IllegalArgumentException(
                        "market session overrides must not contain duplicate dates"
                );
            }
        }
        return Map.copyOf(indexed);
    }
}

package dev.hamm0000.autotrader.trading.marketcalendar.domain;

import java.time.LocalTime;
import java.util.Objects;

/**
 * 개장일의 시작, 종료 시간
 */
public record MarketSession(LocalTime opensAt, LocalTime closesAt) {

    public MarketSession {
        Objects.requireNonNull(opensAt, "opensAt must not be null");
        Objects.requireNonNull(closesAt, "closesAt must not be null");
        if (!opensAt.isBefore(closesAt)) {
            throw new IllegalArgumentException("opensAt must be before closesAt");
        }
    }
}

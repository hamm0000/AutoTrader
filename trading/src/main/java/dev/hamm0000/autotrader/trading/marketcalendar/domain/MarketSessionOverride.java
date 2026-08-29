package dev.hamm0000.autotrader.trading.marketcalendar.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * 정규 거래 시간과 다른 날짜의 세션과 사유입니다.
 */
public record MarketSessionOverride(
        LocalDate date,
        MarketSession session,
        String note
) {

    public MarketSessionOverride {
        Objects.requireNonNull(date, "date must not be null");
        Objects.requireNonNull(session, "session must not be null");
        Objects.requireNonNull(note, "note must not be null");
        if (note.isBlank()) {
            throw new IllegalArgumentException("note must not be blank");
        }
    }
}

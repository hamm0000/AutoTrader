package dev.hamm0000.autotrader.trading.marketcalendar.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;


public record TradingDay(
        LocalDate date,
        TradingDayStatus status,
        Optional<MarketSession> session,
        Optional<String> note,
        Instant verifiedAt
) {

    public TradingDay {
        Objects.requireNonNull(date, "date must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(session, "session must not be null");
        Objects.requireNonNull(note, "note must not be null");
        Objects.requireNonNull(verifiedAt, "verifiedAt must not be null");
        if (status == TradingDayStatus.OPEN && session.isEmpty()) {
            throw new IllegalArgumentException("open trading day must have a market session");
        }
        if (status == TradingDayStatus.CLOSED && session.isPresent()) {
            throw new IllegalArgumentException("closed trading day must not have a market session");
        }
        if (note.isPresent() && note.get().isBlank()) {
            throw new IllegalArgumentException("note must not be blank");
        }
    }

    public static TradingDay open(
            LocalDate date,
            MarketSession session,
            Instant verifiedAt
    ) {
        return open(date, session, Optional.empty(), verifiedAt);
    }

    public static TradingDay open(
            LocalDate date,
            MarketSession session,
            String note,
            Instant verifiedAt
    ) {
        return open(
                date,
                session,
                Optional.of(Objects.requireNonNull(note, "note must not be null")),
                verifiedAt
        );
    }

    private static TradingDay open(
            LocalDate date,
            MarketSession session,
            Optional<String> note,
            Instant verifiedAt
    ) {
        return new TradingDay(
                date,
                TradingDayStatus.OPEN,
                Optional.of(Objects.requireNonNull(session, "session must not be null")),
                note,
                verifiedAt
        );
    }

    public static TradingDay closed(LocalDate date, Instant verifiedAt) {
        return new TradingDay(
                date,
                TradingDayStatus.CLOSED,
                Optional.empty(),
                Optional.empty(),
                verifiedAt
        );
    }

    public static TradingDay closed(LocalDate date, String note, Instant verifiedAt) {
        return new TradingDay(
                date,
                TradingDayStatus.CLOSED,
                Optional.empty(),
                Optional.of(Objects.requireNonNull(note, "note must not be null")),
                verifiedAt
        );
    }

    public boolean isTradingDay() {
        return status == TradingDayStatus.OPEN;
    }
}

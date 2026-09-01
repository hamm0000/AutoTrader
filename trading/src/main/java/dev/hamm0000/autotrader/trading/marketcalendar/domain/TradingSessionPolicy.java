package dev.hamm0000.autotrader.trading.marketcalendar.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

public final class TradingSessionPolicy {

    private final Duration maximumCalendarAge;

    public TradingSessionPolicy(Duration maximumCalendarAge) {
        this.maximumCalendarAge = Objects.requireNonNull(
                maximumCalendarAge,
                "maximumCalendarAge must not be null"
        );
        if (maximumCalendarAge.isZero() || maximumCalendarAge.isNegative()) {
            throw new IllegalArgumentException("maximumCalendarAge must be positive");
        }
    }

    public TradingSessionDecision decide(
            TradingCalendar calendar,
            LocalDate marketDate,
            Instant evaluatedAt
    ) {
        Objects.requireNonNull(calendar, "calendar must not be null");
        Objects.requireNonNull(marketDate, "marketDate must not be null");
        Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");

        Optional<TradingDay> tradingDay = calendar.find(marketDate);
        if (tradingDay.isEmpty()) {
            return TradingSessionDecision.startDegraded(
                    TradingSessionDecision.Reason.CALENDAR_MISSING
            );
        }
        Instant oldestAcceptedVerification = evaluatedAt.minus(maximumCalendarAge);
        if (tradingDay.get().verifiedAt().isBefore(oldestAcceptedVerification)) {
            return TradingSessionDecision.startDegraded(
                    TradingSessionDecision.Reason.CALENDAR_STALE
            );
        }
        if (!tradingDay.get().isTradingDay()) {
            return TradingSessionDecision.stopClosed();
        }
        return TradingSessionDecision.start();
    }
}

package dev.hamm0000.autotrader.trading.marketcalendar.domain;

import java.util.Objects;

// 캘린더의 데이터 신선도가 기준 이상으로 떨어진다면, START_DEGRADED 상태로 런타임을 진행합니다.
public record TradingSessionDecision(Outcome outcome, Reason reason) {

    public TradingSessionDecision {
        Objects.requireNonNull(outcome, "outcome must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        if (!outcome.accepts(reason)) {
            throw new IllegalArgumentException("outcome and reason do not match");
        }
    }

    public static TradingSessionDecision start() {
        return new TradingSessionDecision(Outcome.START, Reason.CONFIRMED_OPEN);
    }

    public static TradingSessionDecision stopClosed() {
        return new TradingSessionDecision(Outcome.STOP_CLOSED, Reason.CONFIRMED_CLOSED);
    }

    public static TradingSessionDecision stopOutsideSession() {
        return new TradingSessionDecision(Outcome.STOP_OUTSIDE_SESSION, Reason.OUTSIDE_SESSION);
    }

    public static TradingSessionDecision startDegraded(Reason reason) {
        return new TradingSessionDecision(Outcome.START_DEGRADED, reason);
    }

    public enum Outcome {
        START,
        STOP_CLOSED,
        STOP_OUTSIDE_SESSION,
        START_DEGRADED;

        private boolean accepts(Reason reason) {
            return switch (this) {
                case START -> reason == Reason.CONFIRMED_OPEN;
                case STOP_CLOSED -> reason == Reason.CONFIRMED_CLOSED;
                case STOP_OUTSIDE_SESSION -> reason == Reason.OUTSIDE_SESSION;
                case START_DEGRADED -> reason == Reason.CALENDAR_MISSING
                        || reason == Reason.CALENDAR_STALE
                        || reason == Reason.CALENDAR_UNAVAILABLE;
            };
        }
    }

    public enum Reason {
        CONFIRMED_OPEN,
        CONFIRMED_CLOSED,
        OUTSIDE_SESSION,
        CALENDAR_MISSING,
        CALENDAR_STALE,
        CALENDAR_UNAVAILABLE
    }
}

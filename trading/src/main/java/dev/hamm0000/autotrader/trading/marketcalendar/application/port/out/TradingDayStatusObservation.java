package dev.hamm0000.autotrader.trading.marketcalendar.application.port.out;

import dev.hamm0000.autotrader.trading.marketcalendar.domain.TradingDayStatus;
import java.time.LocalDate;
import java.util.Objects;

/**
 * 하루치 개장 여부를 나타냅니다.
 */
public record TradingDayStatusObservation(LocalDate date, TradingDayStatus status) {

    public TradingDayStatusObservation {
        Objects.requireNonNull(date, "date must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }
}

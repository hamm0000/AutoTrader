package dev.hamm0000.autotrader.trading.risk.dividend.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public final class DividendRiskPolicy {

    public DividendRiskAssessment assess(
            LocalDate oldestClosingDate,
            LocalDate evaluationDate,
            List<DividendEvent> events
    ) {
        Objects.requireNonNull(oldestClosingDate, "oldestClosingDate must not be null");
        Objects.requireNonNull(evaluationDate, "evaluationDate must not be null");
        Objects.requireNonNull(events, "events must not be null");
        if (!oldestClosingDate.isBefore(evaluationDate)) {
            throw new IllegalArgumentException("oldestClosingDate must be before evaluationDate");
        }

        boolean hasDividendRisk = events.stream()
                .map(Objects::requireNonNull)
                .map(DividendEvent::exDividendDate)
                .anyMatch(exDividendDate -> exDividendDate.isAfter(oldestClosingDate)
                        && !exDividendDate.isAfter(evaluationDate));

        return hasDividendRisk
                ? DividendRiskAssessment.BLOCKED
                : DividendRiskAssessment.CLEAR;
    }
}

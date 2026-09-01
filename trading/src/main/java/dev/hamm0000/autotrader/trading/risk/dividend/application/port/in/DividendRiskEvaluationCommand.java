package dev.hamm0000.autotrader.trading.risk.dividend.application.port.in;

import dev.hamm0000.autotrader.trading.marketdata.domain.RecentClosingPrices;
import java.time.LocalDate;
import java.util.Objects;

public record DividendRiskEvaluationCommand(
        String symbol,
        RecentClosingPrices closingPrices,
        LocalDate evaluationDate
) {

    public DividendRiskEvaluationCommand {
        Objects.requireNonNull(symbol, "symbol must not be null");
        Objects.requireNonNull(closingPrices, "closingPrices must not be null");
        Objects.requireNonNull(evaluationDate, "evaluationDate must not be null");
        if (symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank");
        }
    }
}

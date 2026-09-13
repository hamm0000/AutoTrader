package dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in;

import dev.hamm0000.autotrader.trading.marketdata.domain.CurrentPrice;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.ZScoreEvaluationResult;
import java.util.Objects;

// Z-score 계산 결과에 종목, 현재가를 함께 보존(매수,매도 가격 때문)
public record EvaluatedZScore(
        String symbol,
        CurrentPrice currentPrice,
        ZScoreEvaluationResult analytics
) {

    public EvaluatedZScore {
        Objects.requireNonNull(symbol, "symbol must not be null");
        Objects.requireNonNull(currentPrice, "currentPrice must not be null");
        Objects.requireNonNull(analytics, "analytics must not be null");
        if (symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank");
        }
    }
}

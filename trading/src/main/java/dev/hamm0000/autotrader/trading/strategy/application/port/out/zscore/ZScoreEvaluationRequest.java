package dev.hamm0000.autotrader.trading.strategy.application.port.out.zscore;

import java.util.List;
import java.util.Objects;

public record ZScoreEvaluationRequest(
        String symbol,
        int window,
        List<Double> closePrices,
        double currentPrice
) {

    public ZScoreEvaluationRequest {
        Objects.requireNonNull(symbol, "symbol must not be null");
        // 생성 후 원본 리스트 변경이 반영되거나, 외부에서 이 리스트를 수정하지 못하도록 복사한다
        closePrices = List.copyOf(closePrices);
    }
}

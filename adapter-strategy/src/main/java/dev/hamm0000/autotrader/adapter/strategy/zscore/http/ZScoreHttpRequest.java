package dev.hamm0000.autotrader.adapter.strategy.zscore.http;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.ZScoreEvaluationRequest;

import java.util.List;

record ZScoreHttpRequest(
        String symbol,
        int window,
        @JsonProperty("close_prices") List<Double> closePrices,
        @JsonProperty("now_price") double currentPrice
) {

    static ZScoreHttpRequest from(ZScoreEvaluationRequest request) {
        return new ZScoreHttpRequest(
                request.symbol(),
                request.window(),
                request.closePrices(),
                request.currentPrice()
        );
    }
}

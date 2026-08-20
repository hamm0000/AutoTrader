package dev.hamm0000.autotrader.adapter.kis.query;

import static dev.hamm0000.autotrader.trading.marketdata.application.port.out.MarketDataQueryException.invalidResponse;
import static dev.hamm0000.autotrader.trading.marketdata.application.port.out.MarketDataQueryException.providerUnavailable;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.hamm0000.autotrader.trading.marketdata.domain.CurrentPrice;
import java.math.BigDecimal;

record KisCurrentPriceResponse(
        @JsonProperty("rt_cd") String returnCode,
        KisCurrentPriceOutput output
) {

    CurrentPrice toCurrentPrice() {
        if (returnCode == null || returnCode.isBlank()) {
            throw invalidResponse("KIS 현재가 응답에 rt_cd가 없습니다");
        }
        if (!returnCode.equals("0")) {
            throw providerUnavailable("KIS가 현재가 조회 요청을 처리하지 못했습니다");
        }
        if (output == null) {
            throw invalidResponse("KIS 현재가 응답에 output이 없습니다");
        }
        return output.toCurrentPrice();
    }
}

record KisCurrentPriceOutput(@JsonProperty("stck_prpr") String currentPrice) {

    CurrentPrice toCurrentPrice() {
        if (currentPrice == null || currentPrice.isBlank()) {
            throw invalidResponse("KIS 현재가 응답에 stck_prpr가 없습니다");
        }
        try {
            return new CurrentPrice(new BigDecimal(currentPrice));
        } catch (NumberFormatException exception) {
            throw invalidResponse("KIS 현재가 응답의 가격이 올바르지 않습니다", exception);
        }
    }
}

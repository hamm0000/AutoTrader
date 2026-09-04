package dev.hamm0000.autotrader.adapter.kis.query.account;

import static dev.hamm0000.autotrader.trading.position.application.port.out.PositionQueryException.invalidResponse;
import static dev.hamm0000.autotrader.trading.position.application.port.out.PositionQueryException.providerUnavailable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.hamm0000.autotrader.trading.position.domain.Position;
import java.util.List;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
record KisBalanceResponse(
        @JsonProperty("rt_cd") String returnCode,
        List<KisBalanceItem> output1
) {

    Optional<Position> findPosition(String symbol) {
        validateResponse();
        for (KisBalanceItem item : output1) {
            if (item == null) {
                throw invalidResponse("KIS 잔고 응답의 output1에 빈 항목이 있습니다");
            }
            Position position = item.toPosition();
            if (position.symbol().equals(symbol)) {
                return Optional.of(position);
            }
        }
        return Optional.empty();
    }

    private void validateResponse() {
        if (returnCode == null || returnCode.isBlank()) {
            throw invalidResponse("KIS 잔고 응답에 rt_cd가 없습니다");
        }
        if (!returnCode.equals("0")) {
            throw providerUnavailable("KIS가 잔고 조회 요청을 처리하지 못했습니다");
        }
        if (output1 == null) {
            throw invalidResponse("KIS 잔고 응답에 output1이 없습니다");
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
record KisBalanceItem(
        @JsonProperty("pdno") String symbol,
        @JsonProperty("hldg_qty") String holdingQuantity
) {

    Position toPosition() {
        if (symbol == null || symbol.isBlank()) {
            throw invalidResponse("KIS 잔고 응답에 pdno가 없습니다");
        }
        if (holdingQuantity == null || holdingQuantity.isBlank()) {
            throw invalidResponse("KIS 잔고 응답에 hldg_qty가 없습니다");
        }
        try {
            return new Position(symbol, Long.parseLong(holdingQuantity));
        } catch (IllegalArgumentException exception) {
            throw invalidResponse("KIS 잔고 응답의 보유수량이 올바르지 않습니다", exception);
        }
    }
}

package dev.hamm0000.autotrader.trading.proposal.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Z-score 계산에 사용한 현재가로 보유 종목의 전체 수량을 매도하는 Proposal입니다.
 */
public record SellProposal(
        String symbol,
        long quantity,
        BigDecimal currentPrice
) implements TradeProposal {

    public SellProposal {
        Objects.requireNonNull(symbol, "symbol must not be null");
        Objects.requireNonNull(currentPrice, "currentPrice must not be null");
        if (symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        if (currentPrice.signum() <= 0) {
            throw new IllegalArgumentException("currentPrice must be greater than zero");
        }
    }

    @Override
    public BigDecimal referencePrice() {
        return currentPrice;
    }
}

package dev.hamm0000.autotrader.trading.proposal.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Z-score 계산에 사용한 현재가로 Budget 안에서 정수수량을 매수하는 Proposal입니다.
 */
public record BuyProposal(
        String symbol,
        long quantity,
        BigDecimal currentPrice,
        BigDecimal purchaseBudget
) implements TradeProposal {

    public BuyProposal {
        requireSymbol(symbol);
        Objects.requireNonNull(currentPrice, "currentPrice must not be null");
        Objects.requireNonNull(purchaseBudget, "purchaseBudget must not be null");
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        if (currentPrice.signum() <= 0) {
            throw new IllegalArgumentException("currentPrice must be greater than zero");
        }
        if (purchaseBudget.signum() <= 0) {
            throw new IllegalArgumentException("purchaseBudget must be greater than zero");
        }
        if (currentPrice.multiply(BigDecimal.valueOf(quantity)).compareTo(purchaseBudget) > 0) {
            throw new IllegalArgumentException("proposed amount must not exceed purchaseBudget");
        }
    }

    private static void requireSymbol(String symbol) {
        Objects.requireNonNull(symbol, "symbol must not be null");
        if (symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank");
        }
    }
}

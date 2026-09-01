package dev.hamm0000.autotrader.trading.proposal.domain;

import java.util.Objects;

/**
 * 현재 보유한 종목의 전체 수량을 매도하는 Proposal입니다.
 */
public record SellProposal(String symbol, long quantity) implements TradeProposal {

    public SellProposal {
        Objects.requireNonNull(symbol, "symbol must not be null");
        if (symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
    }
}

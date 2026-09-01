package dev.hamm0000.autotrader.trading.strategy.zscore.domain;

import dev.hamm0000.autotrader.trading.marketdata.domain.CurrentPrice;
import dev.hamm0000.autotrader.trading.position.domain.Position;
import dev.hamm0000.autotrader.trading.proposal.domain.BuyProposal;
import dev.hamm0000.autotrader.trading.proposal.domain.SellProposal;
import dev.hamm0000.autotrader.trading.proposal.domain.TradeProposal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;

/**
 * strategy-zscore가 계산한 Z-score 신호를 거래 Proposal로 변환합니다.
 */
public final class ZScoreTradeProposalPolicy {

    public Optional<TradeProposal> propose(
            String symbol,
            CurrentPrice currentPrice,
            BigDecimal purchaseBudget,
            StrategySignal signal,
            Position position,
            boolean hasOpenOrder
    ) {
        Objects.requireNonNull(symbol, "symbol must not be null");
        Objects.requireNonNull(currentPrice, "currentPrice must not be null");
        Objects.requireNonNull(purchaseBudget, "purchaseBudget must not be null");
        Objects.requireNonNull(signal, "signal must not be null");
        Objects.requireNonNull(position, "position must not be null");
        if (symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank");
        }
        if (currentPrice.value().signum() <= 0) {
            throw new IllegalArgumentException("currentPrice must be greater than zero");
        }
        if (purchaseBudget.signum() <= 0) {
            throw new IllegalArgumentException("purchaseBudget must be greater than zero");
        }
        if (!symbol.equals(position.symbol())) {
            throw new IllegalArgumentException("position symbol must match proposal symbol");
        }
        if (signal == StrategySignal.HOLD || hasOpenOrder) {
            return Optional.empty();
        }

        return switch (signal) {
            case BUY -> proposeBuy(symbol, currentPrice, purchaseBudget, position);
            case SELL -> proposeSell(symbol, position);
            case HOLD -> Optional.empty();
        };
    }

    private static Optional<TradeProposal> proposeBuy(
            String symbol,
            CurrentPrice currentPrice,
            BigDecimal purchaseBudget,
            Position position
    ) {
        if (position.quantity() > 0) {
            return Optional.empty();
        }

        long quantity = purchaseBudget
                .divide(currentPrice.value(), 0, RoundingMode.FLOOR)
                .longValueExact();
        if (quantity == 0) {
            return Optional.empty();
        }
        return Optional.of(new BuyProposal(
                symbol,
                quantity,
                currentPrice.value(),
                purchaseBudget
        ));
    }

    private static Optional<TradeProposal> proposeSell(String symbol, Position position) {
        if (position.quantity() == 0) {
            return Optional.empty();
        }
        return Optional.of(new SellProposal(symbol, position.quantity()));
    }
}

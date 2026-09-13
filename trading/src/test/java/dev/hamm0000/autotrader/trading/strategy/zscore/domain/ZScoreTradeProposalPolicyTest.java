package dev.hamm0000.autotrader.trading.strategy.zscore.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.hamm0000.autotrader.trading.marketdata.domain.CurrentPrice;
import dev.hamm0000.autotrader.trading.position.domain.Position;
import dev.hamm0000.autotrader.trading.proposal.domain.BuyProposal;
import dev.hamm0000.autotrader.trading.proposal.domain.SellProposal;
import dev.hamm0000.autotrader.trading.proposal.domain.TradeProposal;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ZScoreTradeProposalPolicyTest {

    private static final String SYMBOL = "005930";
    private static final BigDecimal PURCHASE_BUDGET = new BigDecimal("10000000");
    private static final CurrentPrice CURRENT_PRICE = currentPrice("72300");

    private final ZScoreTradeProposalPolicy policy = new ZScoreTradeProposalPolicy();

    @Test
    void 보유수량이_없으면_BUDGET_안에서_살_수_있는_정수수량을_제안한다() {
        // when
        Optional<TradeProposal> proposal = propose(StrategySignal.BUY, position(0));

        // then
        assertEquals(
                Optional.of(new BuyProposal(
                        SYMBOL,
                        138,
                        CURRENT_PRICE.value(),
                        PURCHASE_BUDGET
                )),
                proposal
        );
    }

    @Test
    void 한_주_가격이_BUDGET_보다_크면_매수를_제안하지_않는다() {
        // when
        Optional<TradeProposal> proposal = policy.propose(
                SYMBOL,
                currentPrice("10000001"),
                PURCHASE_BUDGET,
                StrategySignal.BUY,
                position(0)
        );

        // then
        assertTrue(proposal.isEmpty());
    }

    @Test
    void 이미_보유한_종목이면_추가_매수를_제안하지_않는다() {

        assertTrue(propose(StrategySignal.BUY, position(1)).isEmpty());
    }

    @Test
    void 보유한_종목에_매도_신호가_오면_전체_수량의_매도를_제안한다() {
        // when
        Optional<TradeProposal> proposal = propose(StrategySignal.SELL, position(7));

        // then
        assertEquals(
                Optional.of(new SellProposal(SYMBOL, 7, CURRENT_PRICE.value())),
                proposal
        );
    }

    @Test
    void 보유수량이_없으면_매도를_제안하지_않는다() {
        assertTrue(propose(StrategySignal.SELL, position(0)).isEmpty());
    }

    @Test
    void HOLD_신호면_주문을_제안하지_않는다() {
        assertTrue(propose(StrategySignal.HOLD, position(0)).isEmpty());
    }

    private Optional<TradeProposal> propose(
            StrategySignal signal,
            Position position
    ) {
        return policy.propose(
                SYMBOL,
                CURRENT_PRICE,
                PURCHASE_BUDGET,
                signal,
                position
        );
    }

    private static Position position(long quantity) {
        return new Position(SYMBOL, quantity);
    }

    private static CurrentPrice currentPrice(String value) {
        return new CurrentPrice(new BigDecimal(value));
    }
}

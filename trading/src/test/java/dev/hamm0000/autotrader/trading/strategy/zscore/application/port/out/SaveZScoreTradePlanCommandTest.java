package dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.hamm0000.autotrader.trading.marketdata.domain.CurrentPrice;
import dev.hamm0000.autotrader.trading.proposal.domain.BuyProposal;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.EvaluatedZScore;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreEvaluationCommand;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreTradePlan;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreTradePlanningCommand;
import dev.hamm0000.autotrader.trading.strategy.zscore.domain.StrategySignal;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SaveZScoreTradePlanCommandTest {

    private static final String SYMBOL = "005930";
    private static final BigDecimal CURRENT_PRICE = new BigDecimal("72300");
    private static final BigDecimal PURCHASE_BUDGET = new BigDecimal("10000000");
    private static final Instant PLANNED_AT = Instant.parse("2026-09-02T00:05:00Z");

    @Test
    void 평가조건과_같은_종목과_BUDGET이면_저장할_수_있다() {
        assertDoesNotThrow(() -> new SaveZScoreTradePlanCommand(
                planningCommand(),
                buyPlan(SYMBOL, PURCHASE_BUDGET),
                PLANNED_AT
        ));
    }

    @Test
    void 평가조건과_계획의_종목이_다르면_저장할_수_없다() {
        assertThrows(IllegalArgumentException.class, () -> new SaveZScoreTradePlanCommand(
                planningCommand(),
                buyPlan("105560", PURCHASE_BUDGET),
                PLANNED_AT
        ));
    }

    @Test
    void 계획조건과_매수제안의_BUDGET이_다르면_저장할_수_없다() {
        assertThrows(IllegalArgumentException.class, () -> new SaveZScoreTradePlanCommand(
                planningCommand(),
                buyPlan(SYMBOL, new BigDecimal("9000000")),
                PLANNED_AT
        ));
    }

    @Test
    void 평가에_사용한_현재가와_주문제안_가격이_다르면_저장할_수_없다() {
        // given
        ZScoreTradePlan plan = buyPlan(SYMBOL, PURCHASE_BUDGET);
        ZScoreTradePlan mismatchedPricePlan = new ZScoreTradePlan(
                plan.evaluation(),
                Optional.of(new BuyProposal(
                        SYMBOL,
                        138,
                        new BigDecimal("72000"),
                        PURCHASE_BUDGET
                ))
        );

        // when & then
        assertThrows(IllegalArgumentException.class, () -> new SaveZScoreTradePlanCommand(
                planningCommand(),
                mismatchedPricePlan,
                PLANNED_AT
        ));
    }

    private static ZScoreTradePlanningCommand planningCommand() {
        return new ZScoreTradePlanningCommand(
                new ZScoreEvaluationCommand(
                        SYMBOL,
                        9,
                        LocalDate.of(2026, 9, 2)
                ),
                PURCHASE_BUDGET
        );
    }

    private static ZScoreTradePlan buyPlan(String symbol, BigDecimal purchaseBudget) {
        EvaluatedZScore evaluation = new EvaluatedZScore(
                symbol,
                new CurrentPrice(CURRENT_PRICE),
                new ZScoreEvaluationResult(
                        StrategySignal.BUY,
                        -2.1,
                        71000.0,
                        1200.0,
                        "평가 결과"
                )
        );
        return new ZScoreTradePlan(
                evaluation,
                Optional.of(new BuyProposal(
                        symbol,
                        purchaseBudget.divideToIntegralValue(CURRENT_PRICE).longValueExact(),
                        CURRENT_PRICE,
                        purchaseBudget
                ))
        );
    }
}

package dev.hamm0000.autotrader.trading.strategy.zscore.application.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.hamm0000.autotrader.trading.marketdata.domain.CurrentPrice;
import dev.hamm0000.autotrader.trading.position.application.port.out.PositionQuery;
import dev.hamm0000.autotrader.trading.position.domain.Position;
import dev.hamm0000.autotrader.trading.proposal.domain.BuyProposal;
import dev.hamm0000.autotrader.trading.proposal.domain.SellProposal;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.EvaluatedZScore;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreEvaluationCommand;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreTradePlan;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreTradePlanningCommand;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.SaveZScoreTradePlanCommand;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.ZScoreEvaluationException;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.ZScoreEvaluationResult;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.service.testdouble.SpyPositionLoader;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.service.testdouble.SpyZScoreTradePlanSaver;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.service.testdouble.SpyZScoreEvaluator;
import dev.hamm0000.autotrader.trading.strategy.zscore.domain.StrategySignal;
import dev.hamm0000.autotrader.trading.strategy.zscore.domain.ZScoreTradeProposalPolicy;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.InstantSource;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ZScoreTradePlanningServiceTest {

    private static final String SYMBOL = "005930";
    private static final int WINDOW = 9;
    private static final LocalDate END_DATE_EXCLUSIVE = LocalDate.of(2026, 9, 2);
    private static final BigDecimal PURCHASE_BUDGET = new BigDecimal("10000000");
    private static final CurrentPrice CURRENT_PRICE = new CurrentPrice(new BigDecimal("72300"));
    private static final Instant PLANNED_AT = Instant.parse("2026-09-02T00:05:00Z");

    @Test
    void 아홉_거래일_종가와_현재가로_평가하고_BUDGET_안의_매수수량을_계획한다() {
        // given
        SpyZScoreEvaluator evaluator = new SpyZScoreEvaluator(evaluation(StrategySignal.BUY));
        SpyPositionLoader positions = new SpyPositionLoader(new Position(SYMBOL, 0));
        SpyZScoreTradePlanSaver plans = new SpyZScoreTradePlanSaver();
        ZScoreTradePlanningService service = service(evaluator, positions, plans);

        // when
        ZScoreTradePlan plan = service.plan(command());

        // then
        assertAll(
                () -> assertEquals(evaluationCommand(), evaluator.command()),
                () -> assertEquals(new PositionQuery(SYMBOL), positions.query()),
                () -> assertEquals(evaluation(StrategySignal.BUY), plan.evaluation()),
                () -> assertEquals(
                        Optional.of(new BuyProposal(
                                SYMBOL,
                                138,
                                CURRENT_PRICE.value(),
                                PURCHASE_BUDGET
                        )),
                        plan.proposal()
                ),
                () -> assertEquals(
                        new SaveZScoreTradePlanCommand(command(), plan, PLANNED_AT),
                        plans.command()
                )
        );
    }

    @Test
    void SELL_신호면_보유수량_전체의_매도를_계획한다() {
        // given
        SpyZScoreEvaluator evaluator = new SpyZScoreEvaluator(evaluation(StrategySignal.SELL));
        SpyPositionLoader positions = new SpyPositionLoader(new Position(SYMBOL, 7));
        SpyZScoreTradePlanSaver plans = new SpyZScoreTradePlanSaver();
        ZScoreTradePlanningService service = service(evaluator, positions, plans);

        // when
        ZScoreTradePlan plan = service.plan(command());

        // then
        assertAll(
                () -> assertEquals(evaluationCommand(), evaluator.command()),
                () -> assertEquals(new PositionQuery(SYMBOL), positions.query()),
                () -> assertEquals(evaluation(StrategySignal.SELL), plan.evaluation()),
                () -> assertEquals(
                        Optional.of(new SellProposal(SYMBOL, 7)),
                        plan.proposal()
                ),
                () -> assertEquals(
                        new SaveZScoreTradePlanCommand(command(), plan, PLANNED_AT),
                        plans.command()
                )
        );
    }

    @Test
    void HOLD면_보유수량을_조회하지_않는다() {
        // given
        SpyZScoreEvaluator evaluator = new SpyZScoreEvaluator(evaluation(StrategySignal.HOLD));
        SpyPositionLoader positions = new SpyPositionLoader(new Position(SYMBOL, 0));
        SpyZScoreTradePlanSaver plans = new SpyZScoreTradePlanSaver();

        // when
        ZScoreTradePlan plan = service(evaluator, positions, plans).plan(command());

        // then
        assertAll(
                () -> assertEquals(SYMBOL, plan.evaluation().symbol()),
                () -> assertEquals(Optional.empty(), plan.proposal()),
                () -> assertEquals(0, positions.loadCount()),
                () -> assertEquals(1, plans.saveCount())
        );
    }

    @Test
    void 평가가_실패하면_보유수량을_조회하지_않고_같은_예외를_전달한다() {
        // given
        ZScoreEvaluationException expected = new ZScoreEvaluationException(
                ZScoreEvaluationException.Reason.SERVER_UNAVAILABLE,
                "전략 평가 실패"
        );
        SpyZScoreEvaluator evaluator = new SpyZScoreEvaluator(evaluation(StrategySignal.HOLD));
        evaluator.failWith(expected);
        SpyPositionLoader positions = new SpyPositionLoader(new Position(SYMBOL, 0));
        SpyZScoreTradePlanSaver plans = new SpyZScoreTradePlanSaver();

        // when
        ZScoreEvaluationException actual = assertThrows(
                ZScoreEvaluationException.class,
                () -> service(evaluator, positions, plans).plan(command())
        );

        // then
        assertAll(
                () -> assertSame(expected, actual),
                () -> assertEquals(1, evaluator.evaluationCount()),
                () -> assertEquals(0, positions.loadCount()),
                () -> assertEquals(0, plans.saveCount())
        );
    }

    private static ZScoreTradePlanningService service(
            SpyZScoreEvaluator evaluator,
            SpyPositionLoader positions,
            SpyZScoreTradePlanSaver plans
    ) {
        return new ZScoreTradePlanningService(
                evaluator,
                positions,
                new ZScoreTradeProposalPolicy(),
                plans,
                InstantSource.fixed(PLANNED_AT)
        );
    }

    private static EvaluatedZScore evaluation(StrategySignal signal) {
        return new EvaluatedZScore(
                SYMBOL,
                CURRENT_PRICE,
                new ZScoreEvaluationResult(signal, -2.1, 71000.0, 1200.0, "평가 결과")
        );
    }

    private static ZScoreTradePlanningCommand command() {
        return new ZScoreTradePlanningCommand(evaluationCommand(), PURCHASE_BUDGET);
    }

    private static ZScoreEvaluationCommand evaluationCommand() {
        return new ZScoreEvaluationCommand(SYMBOL, WINDOW, END_DATE_EXCLUSIVE);
    }
}

package dev.hamm0000.autotrader.trading.strategy.zscore.application.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.hamm0000.autotrader.trading.marketdata.domain.CurrentPrice;
import dev.hamm0000.autotrader.trading.order.application.port.in.OrderPlacementCommand;
import dev.hamm0000.autotrader.trading.order.domain.Order;
import dev.hamm0000.autotrader.trading.order.domain.OrderId;
import dev.hamm0000.autotrader.trading.order.domain.OrderIdempotencyKey;
import dev.hamm0000.autotrader.trading.proposal.domain.BuyProposal;
import dev.hamm0000.autotrader.trading.proposal.domain.TradeProposal;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.EvaluatedZScore;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreEvaluationCommand;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreTradeExecution;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreTradePlan;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreTradePlanningCommand;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.ZScoreEvaluationResult;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.service.testdouble.SpyOrderPlacer;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.service.testdouble.SpyZScoreTradePlanner;
import dev.hamm0000.autotrader.trading.strategy.zscore.domain.StrategySignal;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ZScoreTradeExecutionServiceTest {

    private static final String SYMBOL = "005930";
    private static final LocalDate END_DATE_EXCLUSIVE = LocalDate.of(2026, 9, 2);
    private static final BigDecimal CURRENT_PRICE = new BigDecimal("72300");
    private static final BigDecimal PURCHASE_BUDGET = new BigDecimal("10000000");
    private static final Instant PREPARED_AT = Instant.parse("2026-09-02T00:05:00Z");
    private static final OrderIdempotencyKey IDEMPOTENCY_KEY = new OrderIdempotencyKey(
            "zscore:005930:2026-09-02"
    );

    @Test
    void 매매_제안이_있으면_평가_종목과_종료일로_멱등키를_만들어_주문한다() {
        // given
        TradeProposal proposal = buyProposal();
        ZScoreTradePlan plan = plan(Optional.of(proposal));
        Order order = Order.prepare(
                new OrderId(UUID.fromString("63df7894-b456-4bf7-85cd-706d9d32c7c9")),
                IDEMPOTENCY_KEY,
                proposal,
                PREPARED_AT
        );
        SpyZScoreTradePlanner planner = new SpyZScoreTradePlanner(plan);
        SpyOrderPlacer orderPlacer = new SpyOrderPlacer(order);

        // when
        ZScoreTradeExecution result = service(planner, orderPlacer).execute(command());

        // then
        assertAll(
                () -> assertEquals(command(), planner.command()),
                () -> assertEquals(
                        new OrderPlacementCommand(IDEMPOTENCY_KEY, proposal),
                        orderPlacer.command()
                ),
                () -> assertEquals(new ZScoreTradeExecution(plan, Optional.of(order)), result),
                () -> assertEquals(1, planner.planCount()),
                () -> assertEquals(1, orderPlacer.placeCount())
        );
    }

    @Test
    void 매매_제안이_없으면_주문하지_않고_계획만_반환한다() {
        // given
        ZScoreTradePlan plan = plan(Optional.empty());
        SpyZScoreTradePlanner planner = new SpyZScoreTradePlanner(plan);
        SpyOrderPlacer orderPlacer = new SpyOrderPlacer(order());

        // when
        ZScoreTradeExecution result = service(planner, orderPlacer).execute(command());

        // then
        assertAll(
                () -> assertEquals(new ZScoreTradeExecution(plan, Optional.empty()), result),
                () -> assertEquals(1, planner.planCount()),
                () -> assertEquals(0, orderPlacer.placeCount())
        );
    }

    @Test
    void 계획_수립이_실패하면_주문하지_않고_같은_예외를_전달한다() {
        // given
        RuntimeException expected = new RuntimeException("계획 실패");
        SpyZScoreTradePlanner planner = new SpyZScoreTradePlanner(plan(Optional.empty()));
        planner.failWith(expected);
        SpyOrderPlacer orderPlacer = new SpyOrderPlacer(order());

        // when
        RuntimeException actual = assertThrows(
                RuntimeException.class,
                () -> service(planner, orderPlacer).execute(command())
        );

        // then
        assertAll(
                () -> assertSame(expected, actual),
                () -> assertEquals(1, planner.planCount()),
                () -> assertEquals(0, orderPlacer.placeCount())
        );
    }

    @Test
    void 주문이_실패하면_같은_예외를_전달한다() {
        // given
        RuntimeException expected = new RuntimeException("주문 실패");
        SpyZScoreTradePlanner planner = new SpyZScoreTradePlanner(
                plan(Optional.of(buyProposal()))
        );
        SpyOrderPlacer orderPlacer = new SpyOrderPlacer(order());
        orderPlacer.failWith(expected);

        // when
        RuntimeException actual = assertThrows(
                RuntimeException.class,
                () -> service(planner, orderPlacer).execute(command())
        );

        // then
        assertAll(
                () -> assertSame(expected, actual),
                () -> assertEquals(1, planner.planCount()),
                () -> assertEquals(1, orderPlacer.placeCount())
        );
    }

    private static ZScoreTradeExecutionService service(
            SpyZScoreTradePlanner planner,
            SpyOrderPlacer orderPlacer
    ) {
        return new ZScoreTradeExecutionService(planner, orderPlacer);
    }

    private static ZScoreTradePlanningCommand command() {
        return new ZScoreTradePlanningCommand(
                new ZScoreEvaluationCommand(SYMBOL, 9, END_DATE_EXCLUSIVE),
                PURCHASE_BUDGET
        );
    }

    private static ZScoreTradePlan plan(Optional<TradeProposal> proposal) {
        return new ZScoreTradePlan(
                new EvaluatedZScore(
                        SYMBOL,
                        new CurrentPrice(CURRENT_PRICE),
                        new ZScoreEvaluationResult(
                                proposal.isPresent() ? StrategySignal.BUY : StrategySignal.HOLD,
                                -2.1,
                                71000.0,
                                1200.0,
                                "평가 결과"
                        )
                ),
                proposal
        );
    }

    private static BuyProposal buyProposal() {
        return new BuyProposal(SYMBOL, 138, CURRENT_PRICE, PURCHASE_BUDGET);
    }

    private static Order order() {
        return Order.prepare(
                new OrderId(UUID.fromString("63df7894-b456-4bf7-85cd-706d9d32c7c9")),
                IDEMPOTENCY_KEY,
                buyProposal(),
                PREPARED_AT
        );
    }
}

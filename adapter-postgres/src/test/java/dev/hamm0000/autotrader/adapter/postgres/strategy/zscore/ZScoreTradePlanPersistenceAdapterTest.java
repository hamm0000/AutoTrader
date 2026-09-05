package dev.hamm0000.autotrader.adapter.postgres.strategy.zscore;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.hamm0000.autotrader.adapter.postgres.marketcalendar.PostgresAdapterTestApplication;
import dev.hamm0000.autotrader.trading.marketdata.domain.CurrentPrice;
import dev.hamm0000.autotrader.trading.proposal.domain.BuyProposal;
import dev.hamm0000.autotrader.trading.proposal.domain.SellProposal;
import dev.hamm0000.autotrader.trading.proposal.domain.TradeProposal;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.EvaluatedZScore;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreEvaluationCommand;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreTradePlan;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreTradePlanningCommand;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.SaveZScoreTradePlan;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.SaveZScoreTradePlanCommand;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.ZScoreEvaluationResult;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.ZScoreTradePlanSaveException;
import dev.hamm0000.autotrader.trading.strategy.zscore.domain.StrategySignal;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(
        classes = PostgresAdapterTestApplication.class,
        properties = "spring.jpa.hibernate.ddl-auto=validate"
)
class ZScoreTradePlanPersistenceAdapterTest {

    private static final String SYMBOL = "005930";
    private static final LocalDate END_DATE_EXCLUSIVE = LocalDate.of(2026, 9, 2);
    private static final BigDecimal CURRENT_PRICE = new BigDecimal("72300");
    private static final BigDecimal PURCHASE_BUDGET = new BigDecimal("10000000");
    private static final Instant PLANNED_AT = Instant.parse("2026-09-02T00:05:00Z");

    @Autowired
    private SaveZScoreTradePlan saveZScoreTradePlan;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearTable() {
        jdbcTemplate.execute("TRUNCATE TABLE zscore_trade_plan");
    }

    @Test
    void 매수_계획을_저장하면_평가값과_수량과_예산을_보존한다() {
        // given
        SaveZScoreTradePlanCommand command = command(
                StrategySignal.BUY,
                Optional.of(new BuyProposal(SYMBOL, 138, CURRENT_PRICE, PURCHASE_BUDGET))
        );

        // when
        saveZScoreTradePlan.save(command);

        // then
        StoredPlan stored = storedPlan();
        assertAll(
                () -> assertEquals(SYMBOL, stored.symbol()),
                () -> assertEquals(END_DATE_EXCLUSIVE, stored.endDateExclusive()),
                () -> assertEquals(9, stored.window()),
                () -> assertEquals(PLANNED_AT, stored.plannedAt()),
                () -> assertEquals(0, CURRENT_PRICE.compareTo(stored.currentPrice())),
                () -> assertEquals(-2.1, stored.zScore()),
                () -> assertEquals(71000.0, stored.rollingMean()),
                () -> assertEquals(1200.0, stored.rollingStddev()),
                () -> assertEquals("BUY", stored.signal()),
                () -> assertEquals("평가 결과", stored.reason()),
                () -> assertEquals("BUY", stored.proposalSide()),
                () -> assertEquals(138L, stored.proposedQuantity()),
                () -> assertEquals(0, CURRENT_PRICE.compareTo(stored.proposedPrice())),
                () -> assertEquals(0, PURCHASE_BUDGET.compareTo(stored.purchaseBudget()))
        );
    }

    @Test
    void 관망_계획을_저장하면_종목과_평가값은_보존하고_제안값은_비워둔다() {
        // when
        saveZScoreTradePlan.save(command(StrategySignal.HOLD, Optional.empty()));

        // then
        StoredPlan stored = storedPlan();
        assertAll(
                () -> assertEquals(SYMBOL, stored.symbol()),
                () -> assertEquals("HOLD", stored.signal()),
                () -> assertNull(stored.proposalSide()),
                () -> assertNull(stored.proposedQuantity()),
                () -> assertNull(stored.proposedPrice()),
                () -> assertEquals(0, PURCHASE_BUDGET.compareTo(stored.purchaseBudget()))
        );
    }

    @Test
    void 전량_매도_계획을_저장하면_보유수량을_보존한다() {
        // when
        saveZScoreTradePlan.save(command(
                StrategySignal.SELL,
                Optional.of(new SellProposal(SYMBOL, 7, CURRENT_PRICE))
        ));

        // then
        StoredPlan stored = storedPlan();
        assertAll(
                () -> assertEquals("SELL", stored.proposalSide()),
                () -> assertEquals(7L, stored.proposedQuantity()),
                () -> assertNull(stored.proposedPrice()),
                () -> assertEquals(0, PURCHASE_BUDGET.compareTo(stored.purchaseBudget()))
        );
    }

    @Test
    void 같은_종목과_평가종료일을_다시_저장하면_중복으로_기록하지_않는다() {
        // given
        SaveZScoreTradePlanCommand command = command(StrategySignal.HOLD, Optional.empty());
        saveZScoreTradePlan.save(command);

        // when
        assertThrows(ZScoreTradePlanSaveException.class, () -> saveZScoreTradePlan.save(command));

        // then
        assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM zscore_trade_plan",
                        Integer.class
                )
        );
    }

    private StoredPlan storedPlan() {
        return jdbcTemplate.queryForObject(
                "SELECT symbol, end_date_exclusive, lookback_window, planned_at, current_price, "
                        + "z_score, rolling_mean, rolling_stddev, signal, reason, "
                        + "proposal_side, proposed_quantity, proposed_price, purchase_budget "
                        + "FROM zscore_trade_plan",
                (resultSet, rowNumber) -> new StoredPlan(
                        resultSet.getString("symbol"),
                        resultSet.getObject("end_date_exclusive", LocalDate.class),
                        resultSet.getInt("lookback_window"),
                        resultSet.getObject("planned_at", OffsetDateTime.class).toInstant(),
                        resultSet.getBigDecimal("current_price"),
                        resultSet.getDouble("z_score"),
                        resultSet.getDouble("rolling_mean"),
                        resultSet.getDouble("rolling_stddev"),
                        resultSet.getString("signal"),
                        resultSet.getString("reason"),
                        resultSet.getString("proposal_side"),
                        resultSet.getObject("proposed_quantity", Long.class),
                        resultSet.getBigDecimal("proposed_price"),
                        resultSet.getBigDecimal("purchase_budget")
                )
        );
    }

    private static SaveZScoreTradePlanCommand command(
            StrategySignal signal,
            Optional<TradeProposal> proposal
    ) {
        ZScoreEvaluationCommand evaluationCommand = new ZScoreEvaluationCommand(
                SYMBOL,
                9,
                END_DATE_EXCLUSIVE
        );
        ZScoreTradePlanningCommand planningCommand = new ZScoreTradePlanningCommand(
                evaluationCommand,
                PURCHASE_BUDGET
        );
        EvaluatedZScore evaluation = new EvaluatedZScore(
                SYMBOL,
                new CurrentPrice(CURRENT_PRICE),
                new ZScoreEvaluationResult(
                        signal,
                        -2.1,
                        71000.0,
                        1200.0,
                        "평가 결과"
                )
        );
        return new SaveZScoreTradePlanCommand(
                planningCommand,
                new ZScoreTradePlan(evaluation, proposal),
                PLANNED_AT
        );
    }

    private record StoredPlan(
            String symbol,
            LocalDate endDateExclusive,
            int window,
            Instant plannedAt,
            BigDecimal currentPrice,
            double zScore,
            double rollingMean,
            double rollingStddev,
            String signal,
            String reason,
            String proposalSide,
            Long proposedQuantity,
            BigDecimal proposedPrice,
            BigDecimal purchaseBudget
    ) {
    }
}

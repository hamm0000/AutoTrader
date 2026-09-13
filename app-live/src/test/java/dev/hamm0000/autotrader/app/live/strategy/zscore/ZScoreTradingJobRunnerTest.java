package dev.hamm0000.autotrader.app.live.strategy.zscore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.hamm0000.autotrader.trading.marketcalendar.application.port.in.session.CheckTradingSession;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.in.session.TradingSessionCheckCommand;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.TradingSessionDecision;
import dev.hamm0000.autotrader.trading.marketdata.domain.CurrentPrice;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.EvaluatedZScore;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ExecuteZScoreTrade;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreTradeExecution;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreTradePlan;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreTradePlanningCommand;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.ZScoreEvaluationResult;
import dev.hamm0000.autotrader.trading.strategy.zscore.domain.StrategySignal;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.InstantSource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ZScoreTradingJobRunnerTest {

    private static final Instant NOW = Instant.parse("2026-09-14T06:20:00Z");
    private static final LocalDate MARKET_DATE = LocalDate.of(2026, 9, 14);
    private static final BigDecimal PURCHASE_BUDGET = new BigDecimal("10000000");

    @Test
    void 거래_가능한_세션이면_설정된_종목을_순서대로_한_번씩_실행한다() {
        // given
        StubTradingSessionChecker sessionChecker = new StubTradingSessionChecker(
                TradingSessionDecision.start()
        );
        SpyZScoreTradeExecutor executor = new SpyZScoreTradeExecutor();
        ZScoreTradingJobRunner runner = runner(sessionChecker, executor);

        // when
        runner.run(null);

        // then
        assertThat(sessionChecker.command())
                .isEqualTo(new TradingSessionCheckCommand(MARKET_DATE));
        assertThat(executor.commands())
                .extracting(command -> command.evaluationCommand().symbol())
                .containsExactly("005930", "105560", "055550", "017670", "030200");
        assertThat(executor.commands()).allSatisfy(command -> {
            assertThat(command.evaluationCommand().window()).isEqualTo(9);
            assertThat(command.evaluationCommand().endDateExclusive()).isEqualTo(MARKET_DATE);
            assertThat(command.purchaseBudget()).isEqualByComparingTo(PURCHASE_BUDGET);
        });
    }

    @Test
    void 휴장일이면_거래를_실행하지_않고_정상_종료한다() {
        // given
        SpyZScoreTradeExecutor executor = new SpyZScoreTradeExecutor();
        ZScoreTradingJobRunner runner = runner(
                new StubTradingSessionChecker(TradingSessionDecision.stopClosed()),
                executor
        );

        // when
        runner.run(null);

        // then
        assertThat(executor.commands()).isEmpty();
    }

    @Test
    void 캘린더를_신뢰할_수_없으면_거래를_실행하지_않고_실패를_알린다() {
        // given
        SpyZScoreTradeExecutor executor = new SpyZScoreTradeExecutor();
        ZScoreTradingJobRunner runner = runner(
                new StubTradingSessionChecker(TradingSessionDecision.startDegraded(
                        TradingSessionDecision.Reason.CALENDAR_STALE
                )),
                executor
        );

        // when & then
        assertThatThrownBy(() -> runner.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Z-score 거래 작업을 시작할 수 없습니다: CALENDAR_STALE");
        assertThat(executor.commands()).isEmpty();
    }

    @Test
    void 장_운영_시간이_아니면_거래를_실행하지_않고_실패를_알린다() {
        // given
        SpyZScoreTradeExecutor executor = new SpyZScoreTradeExecutor();
        ZScoreTradingJobRunner runner = runner(
                new StubTradingSessionChecker(TradingSessionDecision.stopOutsideSession()),
                executor
        );

        // when & then
        assertThatThrownBy(() -> runner.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Z-score 거래 작업을 시작할 수 없습니다: OUTSIDE_SESSION");
        assertThat(executor.commands()).isEmpty();
    }

    private static ZScoreTradingJobRunner runner(
            CheckTradingSession sessionChecker,
            ExecuteZScoreTrade executor
    ) {
        return new ZScoreTradingJobRunner(
                sessionChecker,
                executor,
                properties(),
                InstantSource.fixed(NOW)
        );
    }

    private static ZScoreTradingJobProperties properties() {
        return new ZScoreTradingJobProperties(
                9,
                PURCHASE_BUDGET,
                List.of(
                        target("005930", "삼성전자", "반도체"),
                        target("105560", "KB금융", "은행"),
                        target("055550", "신한지주", "은행"),
                        target("017670", "SK텔레콤", "통신"),
                        target("030200", "KT", "통신")
                )
        );
    }

    private static ZScoreTradingJobProperties.Target target(
            String symbol,
            String companyName,
            String sector
    ) {
        return new ZScoreTradingJobProperties.Target(symbol, companyName, sector);
    }

    private static final class StubTradingSessionChecker implements CheckTradingSession {

        private final TradingSessionDecision result;
        private TradingSessionCheckCommand command;

        private StubTradingSessionChecker(TradingSessionDecision result) {
            this.result = result;
        }

        @Override
        public TradingSessionDecision check(TradingSessionCheckCommand command) {
            this.command = command;
            return result;
        }

        private TradingSessionCheckCommand command() {
            return command;
        }
    }

    private static final class SpyZScoreTradeExecutor implements ExecuteZScoreTrade {

        private final List<ZScoreTradePlanningCommand> commands = new ArrayList<>();

        @Override
        public ZScoreTradeExecution execute(ZScoreTradePlanningCommand command) {
            commands.add(command);
            EvaluatedZScore evaluation = new EvaluatedZScore(
                    command.evaluationCommand().symbol(),
                    new CurrentPrice(BigDecimal.ONE),
                    new ZScoreEvaluationResult(
                            StrategySignal.HOLD,
                            0.0,
                            1.0,
                            0.0,
                            "평가 결과"
                    )
            );
            return new ZScoreTradeExecution(
                    new ZScoreTradePlan(evaluation, Optional.empty()),
                    Optional.empty()
            );
        }

        private List<ZScoreTradePlanningCommand> commands() {
            return List.copyOf(commands);
        }
    }
}

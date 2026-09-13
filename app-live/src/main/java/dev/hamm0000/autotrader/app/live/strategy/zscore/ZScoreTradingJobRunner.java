package dev.hamm0000.autotrader.app.live.strategy.zscore;

import dev.hamm0000.autotrader.trading.marketcalendar.application.port.in.session.CheckTradingSession;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.in.session.TradingSessionCheckCommand;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.TradingSessionDecision;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ExecuteZScoreTrade;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreEvaluationCommand;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreTradeExecution;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreTradePlanningCommand;
import java.time.InstantSource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * 저장된 거래 캘린더를 확인한 뒤 설정된 종목의 Z-score 거래를 한 번 실행합니다.
 * 반복 실행 시점은 애플리케이션이 아니라 외부 Scheduler가 결정합니다.
 */
final class ZScoreTradingJobRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ZScoreTradingJobRunner.class);
    private static final ZoneId MARKET_ZONE = ZoneId.of("Asia/Seoul");

    private final CheckTradingSession checkTradingSession;
    private final ExecuteZScoreTrade executeZScoreTrade;
    private final ZScoreTradingJobProperties properties;
    private final InstantSource timeSource;

    ZScoreTradingJobRunner(
            CheckTradingSession checkTradingSession,
            ExecuteZScoreTrade executeZScoreTrade,
            ZScoreTradingJobProperties properties,
            InstantSource timeSource
    ) {
        this.checkTradingSession = Objects.requireNonNull(
                checkTradingSession,
                "checkTradingSession must not be null"
        );
        this.executeZScoreTrade = Objects.requireNonNull(
                executeZScoreTrade,
                "executeZScoreTrade must not be null"
        );
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource must not be null");
    }

    @Override
    public void run(ApplicationArguments arguments) {
        LocalDate marketDate = LocalDate.ofInstant(timeSource.instant(), MARKET_ZONE);
        TradingSessionDecision decision = checkTradingSession.check(
                new TradingSessionCheckCommand(marketDate)
        );

        if (decision.outcome() == TradingSessionDecision.Outcome.STOP_CLOSED) {
            log.info("휴장일이므로 Z-score 거래 작업을 종료합니다 (marketDate={})", marketDate);
            return;
        }
        if (decision.outcome() != TradingSessionDecision.Outcome.START) {
            throw new IllegalStateException(
                    "Z-score 거래 작업을 시작할 수 없습니다: " + decision.reason()
            );
        }

        for (ZScoreTradingJobProperties.Target target : properties.targets()) {
            execute(target, marketDate);
        }
    }

    private void execute(ZScoreTradingJobProperties.Target target, LocalDate marketDate) {
        ZScoreTradeExecution execution = executeZScoreTrade.execute(
                new ZScoreTradePlanningCommand(
                        new ZScoreEvaluationCommand(
                                target.symbol(),
                                properties.lookbackWindow(),
                                marketDate
                        ),
                        properties.purchaseBudget()
                )
        );
        log.info(
                "Z-score 거래 작업 완료 (symbol={}, companyName={}, sector={}, signal={}, orderCreated={})",
                target.symbol(),
                target.companyName(),
                target.sector(),
                execution.plan().evaluation().analytics().signal(),
                execution.order().isPresent()
        );
    }
}

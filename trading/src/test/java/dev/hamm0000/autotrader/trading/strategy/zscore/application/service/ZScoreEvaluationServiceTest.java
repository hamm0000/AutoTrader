package dev.hamm0000.autotrader.trading.strategy.zscore.application.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.hamm0000.autotrader.trading.marketdata.application.port.out.CurrentPriceQuery;
import dev.hamm0000.autotrader.trading.marketdata.application.port.out.MarketDataQueryException;
import dev.hamm0000.autotrader.trading.marketdata.application.port.out.RecentClosingPricesQuery;
import dev.hamm0000.autotrader.trading.marketdata.domain.CurrentPrice;
import dev.hamm0000.autotrader.trading.marketdata.domain.DailyClosingPrice;
import dev.hamm0000.autotrader.trading.marketdata.domain.RecentClosingPrices;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreEvaluationCommand;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.ZScoreEvaluationException;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.ZScoreEvaluationRequest;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.ZScoreEvaluationResult;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.service.testdouble.StubCurrentPriceLoader;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.service.testdouble.StubRecentClosingPricesLoader;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.service.testdouble.StubZScoreStrategy;
import dev.hamm0000.autotrader.trading.strategy.zscore.domain.StrategySignal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ZScoreEvaluationServiceTest {

    private static final String SYMBOL = "005930";
    private static final int WINDOW = 3;
    private static final LocalDate END_DATE_EXCLUSIVE = LocalDate.of(2026, 8, 17);

    @Test
    void 조회한_시장_가격을_전략에_전달하고_평가_결과를_반환한다() {
        // given
        StubRecentClosingPricesLoader closingPrices = new StubRecentClosingPricesLoader(closingPrices());
        StubCurrentPriceLoader currentPrice = new StubCurrentPriceLoader(
                new CurrentPrice(new BigDecimal("103"))
        );
        ZScoreEvaluationResult expected = new ZScoreEvaluationResult(
                StrategySignal.BUY,
                -2.1,
                101.0,
                1.5,
                "진입 기준 충족"
        );
        StubZScoreStrategy strategy = new StubZScoreStrategy(expected);
        ZScoreEvaluationService service = new ZScoreEvaluationService(closingPrices, currentPrice, strategy);

        // when
        ZScoreEvaluationResult actual = service.evaluate(command());

        // then
        assertAll(
                () -> assertEquals(
                        new RecentClosingPricesQuery(SYMBOL, WINDOW, END_DATE_EXCLUSIVE),
                        closingPrices.query()
                ),
                () -> assertEquals(new CurrentPriceQuery(SYMBOL), currentPrice.query()),
                () -> assertEquals(
                        new ZScoreEvaluationRequest(
                                SYMBOL,
                                WINDOW,
                                List.of(100.0, 101.0, 102.0),
                                103.0
                        ),
                        strategy.request()
                ),
                () -> assertEquals(expected, actual)
        );
    }

    @Test
    void 최근_종가_조회가_실패하면_현재가와_전략을_호출하지_않고_예외를_전달한다() {
        // given
        MarketDataQueryException expected = MarketDataQueryException.providerUnavailable("최근 종가 조회 실패");
        StubRecentClosingPricesLoader closingPrices = new StubRecentClosingPricesLoader(closingPrices());
        closingPrices.failWith(expected);
        StubCurrentPriceLoader currentPrice = new StubCurrentPriceLoader(
                new CurrentPrice(new BigDecimal("103"))
        );
        StubZScoreStrategy strategy = new StubZScoreStrategy(result());
        ZScoreEvaluationService service = new ZScoreEvaluationService(closingPrices, currentPrice, strategy);

        // when
        MarketDataQueryException actual = assertThrows(
                MarketDataQueryException.class,
                () -> service.evaluate(command())
        );

        // then
        assertAll(
                () -> assertSame(expected, actual),
                () -> assertEquals(0, currentPrice.loadCount()),
                () -> assertEquals(0, strategy.evaluationCount())
        );
    }

    @Test
    void 현재가_조회가_실패하면_전략을_호출하지_않고_예외를_전달한다() {
        // given
        MarketDataQueryException expected = MarketDataQueryException.providerUnavailable("현재가 조회 실패");
        StubRecentClosingPricesLoader closingPrices = new StubRecentClosingPricesLoader(closingPrices());
        StubCurrentPriceLoader currentPrice = new StubCurrentPriceLoader(
                new CurrentPrice(new BigDecimal("103"))
        );
        currentPrice.failWith(expected);
        StubZScoreStrategy strategy = new StubZScoreStrategy(result());
        ZScoreEvaluationService service = new ZScoreEvaluationService(closingPrices, currentPrice, strategy);

        // when
        MarketDataQueryException actual = assertThrows(
                MarketDataQueryException.class,
                () -> service.evaluate(command())
        );

        // then
        assertAll(
                () -> assertSame(expected, actual),
                () -> assertEquals(0, strategy.evaluationCount())
        );
    }

    @Test
    void 전략_평가가_실패하면_예외를_전달한다() {
        // given
        ZScoreEvaluationException expected = new ZScoreEvaluationException(
                ZScoreEvaluationException.Reason.SERVER_UNAVAILABLE,
                "전략 평가 실패"
        );
        StubRecentClosingPricesLoader closingPrices = new StubRecentClosingPricesLoader(closingPrices());
        StubCurrentPriceLoader currentPrice = new StubCurrentPriceLoader(
                new CurrentPrice(new BigDecimal("103"))
        );
        StubZScoreStrategy strategy = new StubZScoreStrategy(result());
        strategy.failWith(expected);
        ZScoreEvaluationService service = new ZScoreEvaluationService(closingPrices, currentPrice, strategy);

        // when
        ZScoreEvaluationException actual = assertThrows(
                ZScoreEvaluationException.class,
                () -> service.evaluate(command())
        );

        // then
        assertSame(expected, actual);
    }

    private static ZScoreEvaluationCommand command() {
        return new ZScoreEvaluationCommand(SYMBOL, WINDOW, END_DATE_EXCLUSIVE);
    }

    private static RecentClosingPrices closingPrices() {
        return new RecentClosingPrices(List.of(
                closingPrice("2026-08-12", "100"),
                closingPrice("2026-08-13", "101"),
                closingPrice("2026-08-14", "102")
        ));
    }

    private static DailyClosingPrice closingPrice(String tradingDate, String price) {
        return new DailyClosingPrice(LocalDate.parse(tradingDate), new BigDecimal(price));
    }

    private static ZScoreEvaluationResult result() {
        return new ZScoreEvaluationResult(StrategySignal.HOLD, 0.1, 101.0, 1.5, "관망");
    }
}

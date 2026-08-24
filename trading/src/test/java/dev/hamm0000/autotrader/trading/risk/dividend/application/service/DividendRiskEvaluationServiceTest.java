package dev.hamm0000.autotrader.trading.risk.dividend.application.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.hamm0000.autotrader.trading.marketdata.domain.DailyClosingPrice;
import dev.hamm0000.autotrader.trading.marketdata.domain.RecentClosingPrices;
import dev.hamm0000.autotrader.trading.risk.dividend.application.port.in.DividendRiskEvaluationCommand;
import dev.hamm0000.autotrader.trading.risk.dividend.application.port.out.DividendEventQueryException;
import dev.hamm0000.autotrader.trading.risk.dividend.application.port.out.DividendEventsQuery;
import dev.hamm0000.autotrader.trading.risk.dividend.application.service.testdouble.StubDividendEventsLoader;
import dev.hamm0000.autotrader.trading.risk.dividend.domain.DividendEvent;
import dev.hamm0000.autotrader.trading.risk.dividend.domain.DividendRiskAssessment;
import dev.hamm0000.autotrader.trading.risk.dividend.domain.DividendRiskPolicy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class DividendRiskEvaluationServiceTest {

    private static final String SYMBOL = "005930";
    private static final LocalDate OLDEST_CLOSING_DATE = LocalDate.of(2026, 7, 1);
    private static final LocalDate EVALUATION_DATE = LocalDate.of(2026, 8, 17);

    @Test
    void 가장_오래된_종가일과_평가일의_배당_이벤트를_조회해_Risk를_평가한다() {
        // given
        StubDividendEventsLoader events = new StubDividendEventsLoader(List.of(
                new DividendEvent(
                        LocalDate.of(2026, 8, 17),
                        LocalDate.of(2026, 8, 14)
                )
        ));
        DividendRiskEvaluationService service = service(events);

        // when
        DividendRiskAssessment result = service.evaluate(command());

        // then
        assertAll(
                () -> assertEquals(DividendRiskAssessment.BLOCKED, result),
                () -> assertEquals(
                        new DividendEventsQuery(
                                SYMBOL,
                                OLDEST_CLOSING_DATE,
                                EVALUATION_DATE
                        ),
                        events.query()
                )
        );
    }

    @Test
    void 배당일정을_조회할_수_없는_환경이면_SKIP을_반환한다() {
        // given
        StubDividendEventsLoader events = new StubDividendEventsLoader(List.of());
        events.failWith(DividendEventQueryException.unsupportedEnvironment(
                "현재 환경에서는 배당일정을 조회할 수 없습니다"
        ));
        DividendRiskEvaluationService service = service(events);

        // when
        DividendRiskAssessment result = service.evaluate(command());

        // then
        assertEquals(DividendRiskAssessment.SKIP, result);
    }

    @Test
    void 배당일정_조회가_실패하면_예외를_전달한다() {
        // given
        DividendEventQueryException expected = DividendEventQueryException.providerUnavailable(
                "배당일정 조회에 실패했습니다"
        );
        StubDividendEventsLoader events = new StubDividendEventsLoader(List.of());
        events.failWith(expected);
        DividendRiskEvaluationService service = service(events);

        // when
        DividendEventQueryException actual = assertThrows(
                DividendEventQueryException.class,
                () -> service.evaluate(command())
        );

        // then
        assertSame(expected, actual);
    }

    private static DividendRiskEvaluationService service(StubDividendEventsLoader events) {
        return new DividendRiskEvaluationService(events, new DividendRiskPolicy());
    }

    private static DividendRiskEvaluationCommand command() {
        return new DividendRiskEvaluationCommand(
                SYMBOL,
                closingPrices(),
                EVALUATION_DATE
        );
    }

    private static RecentClosingPrices closingPrices() {
        return new RecentClosingPrices(List.of(
                closingPrice("2026-07-01", "100"),
                closingPrice("2026-08-14", "101")
        ));
    }

    private static DailyClosingPrice closingPrice(String tradingDate, String price) {
        return new DailyClosingPrice(LocalDate.parse(tradingDate), new BigDecimal(price));
    }
}

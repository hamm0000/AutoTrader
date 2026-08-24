package dev.hamm0000.autotrader.trading.risk.dividend.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class DividendRiskPolicyTest {

    private static final LocalDate OLDEST_CLOSING_DATE = LocalDate.of(2026, 7, 1);
    private static final LocalDate EVALUATION_DATE = LocalDate.of(2026, 8, 17);

    private final DividendRiskPolicy policy = new DividendRiskPolicy();

    @Test
    void 가장_오래된_종가일과_평가일_사이에_배당락일이_있으면_BLOCKED를_반환한다() {
        // given
        DividendEvent event = dividendEvent("2026-08-17", "2026-08-14");

        // when
        DividendRiskAssessment result = policy.assess(
                OLDEST_CLOSING_DATE,
                EVALUATION_DATE,
                List.of(event)
        );

        // then
        assertEquals(DividendRiskAssessment.BLOCKED, result);
    }

    @Test
    void 여러_배당_이벤트_중_하나라도_가격_구간에_포함되면_BLOCKED를_반환한다() {
        // given
        List<DividendEvent> events = List.of(
                dividendEvent("2026-07-01", "2026-06-30"),
                dividendEvent("2026-08-17", "2026-08-14"),
                dividendEvent("2026-08-19", "2026-08-18")
        );

        // when
        DividendRiskAssessment result = policy.assess(
                OLDEST_CLOSING_DATE,
                EVALUATION_DATE,
                events
        );

        // then
        assertEquals(DividendRiskAssessment.BLOCKED, result);
    }

    @Test
    void 배당_이벤트가_없으면_CLEAR를_반환한다() {
        // when
        DividendRiskAssessment result = policy.assess(
                OLDEST_CLOSING_DATE,
                EVALUATION_DATE,
                List.of()
        );

        // then
        assertEquals(DividendRiskAssessment.CLEAR, result);
    }

    @Test
    void 배당락일이_가장_오래된_종가일과_같으면_CLEAR를_반환한다() {
        // given
        DividendEvent event = dividendEvent("2026-07-02", "2026-07-01");

        // when
        DividendRiskAssessment result = policy.assess(
                OLDEST_CLOSING_DATE,
                EVALUATION_DATE,
                List.of(event)
        );

        // then
        assertEquals(DividendRiskAssessment.CLEAR, result);
    }

    @Test
    void 배당락일이_가장_오래된_종가일보다_과거이면_CLEAR를_반환한다() {
        // given
        DividendEvent event = dividendEvent("2026-07-01", "2026-06-30");

        // when
        DividendRiskAssessment result = policy.assess(
                OLDEST_CLOSING_DATE,
                EVALUATION_DATE,
                List.of(event)
        );

        // then
        assertEquals(DividendRiskAssessment.CLEAR, result);
    }

    @Test
    void 배당락일이_평가일과_같으면_BLOCKED를_반환한다() {
        // given
        DividendEvent event = dividendEvent("2026-08-18", "2026-08-17");

        // when
        DividendRiskAssessment result = policy.assess(
                OLDEST_CLOSING_DATE,
                EVALUATION_DATE,
                List.of(event)
        );

        // then
        assertEquals(DividendRiskAssessment.BLOCKED, result);
    }

    @Test
    void 배당락일이_평가일보다_미래면_CLEAR를_반환한다() {
        // given
        DividendEvent event = dividendEvent("2026-08-19", "2026-08-18");

        // when
        DividendRiskAssessment result = policy.assess(
                OLDEST_CLOSING_DATE,
                EVALUATION_DATE,
                List.of(event)
        );

        // then
        assertEquals(DividendRiskAssessment.CLEAR, result);
    }

    private static DividendEvent dividendEvent(String recordDate, String exDividendDate) {
        return new DividendEvent(LocalDate.parse(recordDate), LocalDate.parse(exDividendDate));
    }
}

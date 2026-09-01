package dev.hamm0000.autotrader.trading.risk.dividend.application.port.out;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DividendEventsQueryTest {

    private static final LocalDate START_DATE_EXCLUSIVE = LocalDate.of(2026, 7, 1);
    private static final LocalDate END_DATE_INCLUSIVE = LocalDate.of(2026, 8, 17);

    @Test
    void 시작일보다_종료일이_늦으면_배당_이벤트를_요청할_수_있다() {
        // when & then
        assertDoesNotThrow(() -> new DividendEventsQuery(
                "005930",
                START_DATE_EXCLUSIVE,
                END_DATE_INCLUSIVE
        ));
    }

    @Test
    void 종목이_비어_있으면_배당_이벤트를_요청할_수_없다() {
        // given
        String symbol = " ";

        // when & then
        assertThrows(IllegalArgumentException.class, () -> new DividendEventsQuery(
                symbol,
                START_DATE_EXCLUSIVE,
                END_DATE_INCLUSIVE
        ));
    }

    @Test
    void 시작일과_종료일이_같으면_배당_이벤트를_요청할_수_없다() {
        // given
        LocalDate endDateInclusive = START_DATE_EXCLUSIVE;

        // when & then
        assertThrows(IllegalArgumentException.class, () -> new DividendEventsQuery(
                "005930",
                START_DATE_EXCLUSIVE,
                endDateInclusive
        ));
    }

    @Test
    void 시작일이_종료일보다_늦으면_배당_이벤트를_요청할_수_없다() {
        // given
        LocalDate startDateExclusive = END_DATE_INCLUSIVE.plusDays(1);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> new DividendEventsQuery(
                "005930",
                startDateExclusive,
                END_DATE_INCLUSIVE
        ));
    }
}

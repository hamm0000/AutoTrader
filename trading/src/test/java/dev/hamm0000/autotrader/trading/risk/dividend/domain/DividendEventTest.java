package dev.hamm0000.autotrader.trading.risk.dividend.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DividendEventTest {

    private static final LocalDate RECORD_DATE = LocalDate.of(2026, 8, 17);

    @Test
    void 배당락일이_기준일보다_이르면_배당_이벤트를_만들_수_있다() {
        // given
        LocalDate exDividendDate = LocalDate.of(2026, 8, 14);

        // when & then
        assertDoesNotThrow(() -> new DividendEvent(RECORD_DATE, exDividendDate));
    }

    @Test
    void 배당락일이_기준일과_같으면_배당_이벤트를_만들_수_없다() {
        // given
        LocalDate exDividendDate = RECORD_DATE;

        // when & then
        assertThrows(
                IllegalArgumentException.class,
                () -> new DividendEvent(RECORD_DATE, exDividendDate)
        );
    }

    @Test
    void 배당락일이_기준일보다_늦으면_배당_이벤트를_만들_수_없다() {
        // given
        LocalDate exDividendDate = RECORD_DATE.plusDays(1);

        // when & then
        assertThrows(
                IllegalArgumentException.class,
                () -> new DividendEvent(RECORD_DATE, exDividendDate)
        );
    }
}

package dev.hamm0000.autotrader.trading.marketcalendar.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CalendarPeriodTest {

    @Test
    void 시작일과_종료일을_포함하는_캘린더_범위를_만든다() {
        // when
        CalendarPeriod period = new CalendarPeriod(
                LocalDate.parse("2026-08-14"),
                LocalDate.parse("2026-08-16")
        );

        // then
        assertAll(
                () -> assertEquals(3, period.dayCount()),
                () -> assertTrue(period.contains(LocalDate.parse("2026-08-14"))),
                () -> assertTrue(period.contains(LocalDate.parse("2026-08-16"))),
                () -> assertFalse(period.contains(LocalDate.parse("2026-08-17")))
        );
    }

    @Test
    void 종료일이_시작일보다_과거면_캘린더_범위를_만들_수_없다() {
        // when & then
        assertThrows(
                IllegalArgumentException.class,
                () -> new CalendarPeriod(
                        LocalDate.parse("2026-08-16"),
                        LocalDate.parse("2026-08-14")
                )
        );
    }
}

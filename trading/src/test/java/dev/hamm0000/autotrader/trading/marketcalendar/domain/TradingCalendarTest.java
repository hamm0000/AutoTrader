package dev.hamm0000.autotrader.trading.marketcalendar.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TradingCalendarTest {

    private static final Instant VERIFIED_AT = Instant.parse("2026-08-13T09:30:00Z");
    private static final MarketSession REGULAR_SESSION = new MarketSession(
            LocalTime.of(9, 0),
            LocalTime.of(15, 30)
    );

    @Test
    void 조회_범위의_날짜별_거래_일정을_보관한다() {
        // given
        TradingCalendar calendar = calendar();

        // when & then
        assertAll(
                () -> assertEquals(
                        Optional.of(openDay("2026-08-14")),
                        calendar.find(LocalDate.parse("2026-08-14"))
                ),
                () -> assertEquals(
                        Optional.of(closedDay("2026-08-15")),
                        calendar.find(LocalDate.parse("2026-08-15"))
                ),
                () -> assertEquals(
                        Optional.empty(),
                        calendar.find(LocalDate.parse("2026-08-18"))
                )
        );
    }

    @Test
    void 날짜가_누락된_캘린더는_만들_수_없다() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new TradingCalendar(
                        period("2026-08-14", "2026-08-16"),
                        List.of(
                                openDay("2026-08-14"),
                                closedDay("2026-08-16")
                        )
                )
        );
    }

    @Test
    void 날짜가_중복된_캘린더는_만들_수_없다() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new TradingCalendar(
                        period("2026-08-14", "2026-08-16"),
                        List.of(
                                openDay("2026-08-14"),
                                closedDay("2026-08-15"),
                                closedDay("2026-08-15")
                        )
                )
        );
    }

    @Test
    void 기준일_이전의_가장_가까운_거래일을_찾는다() {
        // given
        TradingCalendar calendar = calendar();

        // when & then
        assertAll(
                () -> assertEquals(
                        Optional.of(LocalDate.parse("2026-08-14")),
                        calendar.findPreviousTradingDay(LocalDate.parse("2026-08-17"))
                ),
                () -> assertEquals(
                        Optional.of(LocalDate.parse("2026-08-17")),
                        calendar.findPreviousTradingDay(LocalDate.parse("2026-08-18"))
                ),
                () -> assertEquals(
                        Optional.empty(),
                        calendar.findPreviousTradingDay(LocalDate.parse("2026-08-14"))
                )
        );
    }

    private static TradingCalendar calendar() {
        return new TradingCalendar(
                period("2026-08-14", "2026-08-17"),
                List.of(
                        openDay("2026-08-14"),
                        closedDay("2026-08-15"),
                        closedDay("2026-08-16"),
                        openDay("2026-08-17")
                )
        );
    }

    private static CalendarPeriod period(String from, String through) {
        return new CalendarPeriod(LocalDate.parse(from), LocalDate.parse(through));
    }

    private static TradingDay openDay(String date) {
        return TradingDay.open(LocalDate.parse(date), REGULAR_SESSION, VERIFIED_AT);
    }

    private static TradingDay closedDay(String date) {
        return TradingDay.closed(LocalDate.parse(date), VERIFIED_AT);
    }
}

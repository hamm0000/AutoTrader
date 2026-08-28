package dev.hamm0000.autotrader.trading.marketcalendar.application.port.out;

import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.hamm0000.autotrader.trading.marketcalendar.domain.CalendarPeriod;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.TradingDayStatus;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class TradingDayStatusesTest {

    @Test
    void 조회_범위의_날짜가_누락되면_만들_수_없다() {
        // given
        LocalDate from = LocalDate.parse("2026-08-14");
        LocalDate through = LocalDate.parse("2026-08-16");

        // when & then
        assertThrows(
                IllegalArgumentException.class,
                () -> new TradingDayStatuses(
                        new CalendarPeriod(from, through),
                        List.of(
                                status("2026-08-14", TradingDayStatus.OPEN),
                                status("2026-08-16", TradingDayStatus.CLOSED)
                        )
                )
        );
    }

    @Test
    void 조회_범위의_날짜가_중복되면_만들_수_없다() {
        // given
        LocalDate from = LocalDate.parse("2026-08-14");
        LocalDate through = LocalDate.parse("2026-08-16");

        // when & then
        assertThrows(
                IllegalArgumentException.class,
                () -> new TradingDayStatuses(
                        new CalendarPeriod(from, through),
                        List.of(
                                status("2026-08-14", TradingDayStatus.OPEN),
                                status("2026-08-15", TradingDayStatus.CLOSED),
                                status("2026-08-15", TradingDayStatus.CLOSED)
                        )
                )
        );
    }

    private static TradingDayStatusObservation status(
            String date,
            TradingDayStatus status
    ) {
        return new TradingDayStatusObservation(LocalDate.parse(date), status);
    }
}

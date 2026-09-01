package dev.hamm0000.autotrader.trading.marketcalendar.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TradingDayTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 12);
    private static final Instant VERIFIED_AT = Instant.parse("2026-11-11T09:30:00Z");
    private static final MarketSession SESSION = new MarketSession(
            LocalTime.of(10, 0),
            LocalTime.of(15, 30)
    );

    @Test
    void 거래일은_세션_시각을_함께_가진다() {
        // when
        TradingDay tradingDay = TradingDay.open(
                DATE,
                SESSION,
                "수능",
                VERIFIED_AT
        );

        // then
        assertAll(
                () -> assertEquals(DATE, tradingDay.date()),
                () -> assertEquals(TradingDayStatus.OPEN, tradingDay.status()),
                () -> assertEquals(Optional.of(SESSION), tradingDay.session()),
                () -> assertEquals(Optional.of("수능"), tradingDay.note()),
                () -> assertEquals(VERIFIED_AT, tradingDay.verifiedAt()),
                () -> assertTrue(tradingDay.isTradingDay())
        );
    }

    @Test
    void 휴장일은_세션_시각을_가지지_않는다() {
        // when
        TradingDay closedDay = TradingDay.closed(DATE, VERIFIED_AT);

        // then
        assertAll(
                () -> assertEquals(TradingDayStatus.CLOSED, closedDay.status()),
                () -> assertEquals(Optional.empty(), closedDay.session()),
                () -> assertEquals(Optional.empty(), closedDay.note()),
                () -> assertFalse(closedDay.isTradingDay())
        );
    }

    @Test
    void 거래일에_세션이_없으면_만들_수_없다() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new TradingDay(
                        DATE,
                        TradingDayStatus.OPEN,
                        Optional.empty(),
                        Optional.empty(),
                        VERIFIED_AT
                )
        );
    }

    @Test
    void 휴장일에_세션이_있으면_만들_수_없다() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new TradingDay(
                        DATE,
                        TradingDayStatus.CLOSED,
                        Optional.of(SESSION),
                        Optional.empty(),
                        VERIFIED_AT
                )
        );
    }

    @Test
    void 비어_있는_일정_설명은_저장할_수_없다() {
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> TradingDay.open(DATE, SESSION, " ", VERIFIED_AT)
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> TradingDay.closed(DATE, "", VERIFIED_AT)
                )
        );
    }
}

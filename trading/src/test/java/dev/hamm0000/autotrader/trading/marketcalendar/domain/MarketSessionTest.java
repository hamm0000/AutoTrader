package dev.hamm0000.autotrader.trading.marketcalendar.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class MarketSessionTest {

    @Test
    void 시작과_종료_시각으로_거래_세션을_만든다() {
        // when
        MarketSession session = new MarketSession(
                LocalTime.of(10, 0),
                LocalTime.of(15, 30)
        );

        // then
        assertAll(
                () -> assertEquals(LocalTime.of(10, 0), session.opensAt()),
                () -> assertEquals(LocalTime.of(15, 30), session.closesAt())
        );
    }

    @Test
    void 종료_시각이_시작_시각과_같으면_거래_세션을_만들_수_없다() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new MarketSession(LocalTime.of(9, 0), LocalTime.of(9, 0))
        );
    }

    @Test
    void 종료_시각이_시작_시각보다_빠르면_거래_세션을_만들_수_없다() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new MarketSession(LocalTime.of(9, 0), LocalTime.of(8, 59))
        );
    }
}

package dev.hamm0000.autotrader.trading.marketdata.application.port.out;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CurrentPriceQueryTest {

    @Test
    void 종목이_비어_있으면_현재가를_요청할_수_없다() {
        // given
        String symbol = " ";

        // when & then
        assertThrows(IllegalArgumentException.class, () -> new CurrentPriceQuery(symbol));
    }
}

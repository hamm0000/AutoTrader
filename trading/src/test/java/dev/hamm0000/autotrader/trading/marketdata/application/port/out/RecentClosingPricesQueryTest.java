package dev.hamm0000.autotrader.trading.marketdata.application.port.out;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class RecentClosingPricesQueryTest {

    private static final LocalDate END_DATE_EXCLUSIVE = LocalDate.of(2026, 8, 16);

    @Test
    void 한_거래일의_종가부터_요청할_수_있다() {
        assertDoesNotThrow(() -> new RecentClosingPricesQuery(
                "005930",
                1,
                END_DATE_EXCLUSIVE
        ));
    }

    @Test
    void 종목이_비어_있으면_종가를_요청할_수_없다() {
        assertThrows(IllegalArgumentException.class, () -> new RecentClosingPricesQuery(
                " ",
                1,
                END_DATE_EXCLUSIVE
        ));
    }

    @Test
    void 요청한_거래일_수가_0이면_종가를_요청할_수_없다() {
        assertThrows(IllegalArgumentException.class, () -> new RecentClosingPricesQuery(
                "005930",
                0,
                END_DATE_EXCLUSIVE
        ));
    }
}

package dev.hamm0000.autotrader.trading.marketdata.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecentClosingPricesTest {

    @Test
    void 생성_후_원본_목록이_바뀌어도_최근_종가는_변하지_않는다() {
        List<DailyClosingPrice> source = new ArrayList<>(List.of(
                closingPrice("2026-08-13", "70000"),
                closingPrice("2026-08-14", "71000")
        ));
        RecentClosingPrices closingPrices = new RecentClosingPrices(source);

        source.add(closingPrice("2026-08-15", "72000"));

        assertEquals(2, closingPrices.values().size());
    }

    @Test
    void 최근_종가가_최신일부터_정렬되어_있으면_만들_수_없다() {
        List<DailyClosingPrice> newestFirst = List.of(
                closingPrice("2026-08-14", "71000"),
                closingPrice("2026-08-13", "70000")
        );

        assertThrows(IllegalArgumentException.class, () -> new RecentClosingPrices(newestFirst));
    }

    @Test
    void 같은_거래일의_종가가_중복되면_만들_수_없다() {
        List<DailyClosingPrice> duplicatedDate = List.of(
                closingPrice("2026-08-14", "70000"),
                closingPrice("2026-08-14", "71000")
        );

        assertThrows(IllegalArgumentException.class, () -> new RecentClosingPrices(duplicatedDate));
    }

    private static DailyClosingPrice closingPrice(String tradingDate, String price) {
        return new DailyClosingPrice(LocalDate.parse(tradingDate), new BigDecimal(price));
    }
}

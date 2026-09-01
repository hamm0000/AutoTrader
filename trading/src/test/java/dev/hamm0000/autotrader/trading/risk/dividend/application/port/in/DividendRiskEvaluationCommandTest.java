package dev.hamm0000.autotrader.trading.risk.dividend.application.port.in;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.hamm0000.autotrader.trading.marketdata.domain.DailyClosingPrice;
import dev.hamm0000.autotrader.trading.marketdata.domain.RecentClosingPrices;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class DividendRiskEvaluationCommandTest {

    private static final RecentClosingPrices CLOSING_PRICES = new RecentClosingPrices(List.of(
            new DailyClosingPrice(LocalDate.of(2026, 8, 14), new BigDecimal("70000"))
    ));
    private static final LocalDate EVALUATION_DATE = LocalDate.of(2026, 8, 17);

    @Test
    void 종목과_가격_구간이_있으면_배당_Risk를_평가할_수_있다() {
        // when
        DividendRiskEvaluationCommand command = new DividendRiskEvaluationCommand(
                "005930",
                CLOSING_PRICES,
                EVALUATION_DATE
        );

        // then
        assertAll(
                () -> assertEquals("005930", command.symbol()),
                () -> assertEquals(CLOSING_PRICES, command.closingPrices()),
                () -> assertEquals(EVALUATION_DATE, command.evaluationDate())
        );
    }

    @Test
    void 종목이_비어_있으면_배당_Risk를_평가할_수_없다() {
        // given
        String symbol = " ";

        // when & then
        assertThrows(IllegalArgumentException.class, () -> new DividendRiskEvaluationCommand(
                symbol,
                CLOSING_PRICES,
                EVALUATION_DATE
        ));
    }
}

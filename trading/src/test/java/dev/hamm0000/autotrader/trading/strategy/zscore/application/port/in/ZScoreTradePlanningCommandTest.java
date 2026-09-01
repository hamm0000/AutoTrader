package dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ZScoreTradePlanningCommandTest {

    private static final ZScoreEvaluationCommand EVALUATION_COMMAND = new ZScoreEvaluationCommand(
            "005930",
            9,
            LocalDate.of(2026, 9, 2)
    );

    @Test
    void 기존_Z_score_평가조건과_BUDGET으로_거래를_계획한다() {
        ZScoreTradePlanningCommand command = assertDoesNotThrow(
                () -> command(new BigDecimal("10000000"))
        );

        assertEquals(EVALUATION_COMMAND, command.evaluationCommand());
    }

    @Test
    void BUDGET이_0원이면_거래를_계획할_수_없다() {
        assertThrows(IllegalArgumentException.class, () -> command(BigDecimal.ZERO));
    }


    private static ZScoreTradePlanningCommand command(BigDecimal purchaseBudget) {
        return new ZScoreTradePlanningCommand(EVALUATION_COMMAND, purchaseBudget);
    }
}

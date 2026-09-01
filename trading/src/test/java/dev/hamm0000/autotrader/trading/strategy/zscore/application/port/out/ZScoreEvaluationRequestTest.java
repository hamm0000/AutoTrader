package dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZScoreEvaluationRequestTest {

    @Test
    void 생성_후_원본_종가_리스트를_변경해도_ZScore_입력의_종가는_변하지_않는다() {
        // given
        // 방어적 복사를 검증하려면 원본이 mutable해야 함
        List<Double> closePrices = new ArrayList<>(List.of(99.0, 100.0, 101.0));
        ZScoreEvaluationRequest request = new ZScoreEvaluationRequest(
                "005930",
                3,
                closePrices,
                102.0
        );

        // when
        closePrices.add(103.0);

        // then
        assertEquals(List.of(99.0, 100.0, 101.0), request.closePrices());
    }
}

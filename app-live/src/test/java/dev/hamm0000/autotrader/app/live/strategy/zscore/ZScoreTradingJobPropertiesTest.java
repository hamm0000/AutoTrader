package dev.hamm0000.autotrader.app.live.strategy.zscore;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ZScoreTradingJobPropertiesTest {

    @Test
    void 거래_대상_종목이_중복되면_설정할_수_없다() {
        // given
        ZScoreTradingJobProperties.Target samsung = new ZScoreTradingJobProperties.Target(
                "005930",
                "삼성전자",
                "반도체"
        );
        ZScoreTradingJobProperties properties = new ZScoreTradingJobProperties(
                9,
                new BigDecimal("10000000"),
                List.of(samsung, samsung)
        );

        // when & then
        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Z-score 거래 대상 종목은 중복될 수 없습니다: 005930");
    }
}

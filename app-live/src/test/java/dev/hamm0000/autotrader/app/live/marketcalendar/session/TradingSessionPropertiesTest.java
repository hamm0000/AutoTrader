package dev.hamm0000.autotrader.app.live.marketcalendar.session;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class TradingSessionPropertiesTest {

    @Test
    void 캘린더_최대_신뢰_기간이_0이면_설정할_수_없다() {
        // given
        TradingSessionProperties properties = new TradingSessionProperties(Duration.ZERO);

        // when & then
        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("거래 캘린더 최대 신뢰 기간은 0보다 커야 합니다");
    }
}

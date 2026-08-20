package dev.hamm0000.autotrader.app.live.strategy.zscore;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class ZScorePropertiesTest {

    @Test
    void ZScore_Server_주소가_유효하지_않으면_설정할_수_없다() {
        // given
        ZScoreProperties properties = new ZScoreProperties(
                URI.create("/relative-path"),
                Duration.ofSeconds(3)
        );

        // when & then
        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Z-score Server Base URL이 올바르지 않습니다");
    }

    @Test
    void ZScore_Server_timeout이_0이면_설정할_수_없다() {
        // given
        ZScoreProperties properties = new ZScoreProperties(
                URI.create("http://zscore.test"),
                Duration.ZERO
        );

        // when & then
        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Z-score Server timeout은 0보다 커야 합니다");
    }
}

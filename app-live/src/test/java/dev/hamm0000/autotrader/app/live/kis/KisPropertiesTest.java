package dev.hamm0000.autotrader.app.live.kis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class KisPropertiesTest {

    private static final URI PAPER_BASE_URL =
            URI.create("https://openapivts.koreainvestment.com:29443");

    @Test
    void 모의투자_주소와_키가_있으면_KIS를_설정할_수_있다() {
        // given
        KisProperties properties = new KisProperties(
                PAPER_BASE_URL,
                "test-app-key",
                "test-app-secret",
                Duration.ofSeconds(5));

        // when & then
        assertThatCode(properties::validate).doesNotThrowAnyException();
    }

    @Test
    void 모의투자_URL이_아니면_KIS를_설정할_수_없다() {
        // given
        KisProperties properties = new KisProperties(
                URI.create("https://notpaper.com"),
                "test-app-key",
                "test-app-secret",
                Duration.ofSeconds(5));

        // when & then
        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("KIS 인증은 모의투자 URL만 허용합니다");
    }

    @Test
    void 자격증명이_비어_있으면_KIS를_설정할_수_없다() {
        // given
        KisProperties properties = new KisProperties(
                PAPER_BASE_URL,
                "",
                "",
                Duration.ofSeconds(5));

        // when & then
        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("KIS 모의투자 자격증명이 필요합니다");
    }

    @Test
    void 설정을_문자열로_표현해도_자격증명은_노출하지_않는다() {
        // given
        KisProperties properties = new KisProperties(
                PAPER_BASE_URL,
                "sensitive-app-key",
                "sensitive-app-secret",
                Duration.ofSeconds(5));

        // when
        String result = properties.toString();

        // then
        assertThat(result)
                .doesNotContain("sensitive-app-key", "sensitive-app-secret");
    }
}

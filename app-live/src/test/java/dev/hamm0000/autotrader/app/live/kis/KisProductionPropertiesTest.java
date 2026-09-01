package dev.hamm0000.autotrader.app.live.kis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class KisProductionPropertiesTest {

    private static final URI PRODUCTION_BASE_URL =
            URI.create("https://openapi.koreainvestment.com:9443");

    @Test
    void 실전투자_주소와_키가_있으면_KIS를_설정할_수_있다() {
        // given
        KisProductionProperties properties = properties(PRODUCTION_BASE_URL);

        // when & then
        assertThatCode(properties::validate).doesNotThrowAnyException();
    }

    @Test
    void 실전투자_URL이_아니면_KIS를_설정할_수_없다() {
        // given
        KisProductionProperties properties = properties(
                URI.create("https://openapivts.koreainvestment.com:29443")
        );

        // when & then
        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("KIS 실전투자 URL만 허용합니다");
    }

    @Test
    void 자격증명이_비어_있으면_실전투자_KIS를_설정할_수_없다() {
        // given
        KisProductionProperties properties = new KisProductionProperties(
                PRODUCTION_BASE_URL,
                "",
                "",
                Duration.ofSeconds(5)
        );

        // when & then
        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("KIS 실전투자 자격증명이 필요합니다");
    }

    @Test
    void 설정을_문자열로_표현해도_실전투자_자격증명은_노출하지_않는다() {
        // given
        KisProductionProperties properties = new KisProductionProperties(
                PRODUCTION_BASE_URL,
                "sensitive-production-app-key",
                "sensitive-production-app-secret",
                Duration.ofSeconds(5)
        );

        // when
        String result = properties.toString();

        // then
        assertThat(result).doesNotContain(
                "sensitive-production-app-key",
                "sensitive-production-app-secret"
        );
    }

    private static KisProductionProperties properties(URI baseUrl) {
        return new KisProductionProperties(
                baseUrl,
                "test-production-app-key",
                "test-production-app-secret",
                Duration.ofSeconds(5)
        );
    }
}

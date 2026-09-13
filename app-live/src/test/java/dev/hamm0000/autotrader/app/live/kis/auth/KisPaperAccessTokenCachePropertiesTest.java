package dev.hamm0000.autotrader.app.live.kis.auth;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class KisPaperAccessTokenCachePropertiesTest {

    @Test
    void 파일_캐시가_꺼져_있으면_경로가_없어도_된다() {
        // given
        KisPaperAccessTokenCacheProperties properties =
                new KisPaperAccessTokenCacheProperties(false, null);

        // when & then
        assertThatCode(properties::validate).doesNotThrowAnyException();
    }

    @Test
    void 파일_캐시를_켜면_저장_경로가_필요하다() {
        // given
        KisPaperAccessTokenCacheProperties properties =
                new KisPaperAccessTokenCacheProperties(true, null);

        // when & then
        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("KIS 모의투자 AccessToken 파일 캐시 경로가 필요합니다");
    }

    @Test
    void 파일_캐시를_켜면_빈_저장_경로를_사용할_수_없다() {
        // given
        KisPaperAccessTokenCacheProperties properties =
                new KisPaperAccessTokenCacheProperties(true, Path.of(""));

        // when & then
        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("KIS 모의투자 AccessToken 파일 캐시 경로가 필요합니다");
    }

    @Test
    void 파일_캐시를_켜고_저장_경로가_있으면_설정할_수_있다() {
        // given
        KisPaperAccessTokenCacheProperties properties =
                new KisPaperAccessTokenCacheProperties(
                        true,
                        Path.of("/tmp/autotrader/kis/token.json")
                );

        // when & then
        assertThatCode(properties::validate).doesNotThrowAnyException();
    }
}

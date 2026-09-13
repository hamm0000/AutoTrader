package dev.hamm0000.autotrader.app.live.kis.auth;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("autotrader.kis.paper.access-token-cache")
record KisPaperAccessTokenCacheProperties(boolean enabled, Path path) {

    void validate() {
        if (enabled && (path == null || path.toString().isBlank())) {
            throw new IllegalStateException(
                    "KIS 모의투자 AccessToken 파일 캐시 경로가 필요합니다"
            );
        }
    }
}

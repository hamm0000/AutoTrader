package dev.hamm0000.autotrader.app.live.strategy.zscore;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("autotrader.strategy.zscore")
record ZScoreProperties(URI baseUrl, Duration requestTimeout) {

    void validate() {
        if (baseUrl == null
                || !baseUrl.isAbsolute()
                || baseUrl.getHost() == null
                || !("http".equals(baseUrl.getScheme()) || "https".equals(baseUrl.getScheme()))) {
            throw new IllegalStateException("Z-score Server Base URL이 올바르지 않습니다");
        }
        if (requestTimeout == null || requestTimeout.isZero() || requestTimeout.isNegative()) {
            throw new IllegalStateException("Z-score Server timeout은 0보다 커야 합니다");
        }
    }
}

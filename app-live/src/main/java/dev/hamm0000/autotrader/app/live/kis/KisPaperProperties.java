package dev.hamm0000.autotrader.app.live.kis;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("autotrader.kis.paper")
public record KisPaperProperties(
        URI baseUrl,
        String appKey,
        String appSecret,
        Duration requestTimeout) {

    private static final URI PAPER_BASE_URL =
            URI.create("https://openapivts.koreainvestment.com:29443");

    public void validate() {
        if (!PAPER_BASE_URL.equals(baseUrl)) {
            throw new IllegalStateException("KIS 인증은 모의투자 URL만 허용합니다");
        }
        if (appKey == null || appKey.isBlank() || appSecret == null || appSecret.isBlank()) {
            throw new IllegalStateException("KIS 모의투자 자격증명이 필요합니다");
        }
        if (requestTimeout == null || requestTimeout.isZero() || requestTimeout.isNegative()) {
            throw new IllegalStateException("KIS 요청 timeout은 0보다 커야 합니다");
        }
    }

    @Override
    public String toString() {
        return "KisPaperProperties[baseUrl=" + baseUrl
                + ", appKey=***, appSecret=***, requestTimeout=" + requestTimeout + "]";
    }
}

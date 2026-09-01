package dev.hamm0000.autotrader.app.live.kis;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("autotrader.kis.production")
public record KisProductionProperties(
        URI baseUrl,
        String appKey,
        String appSecret,
        Duration requestTimeout
) {

    private static final URI PRODUCTION_BASE_URL =
            URI.create("https://openapi.koreainvestment.com:9443");

    public void validate() {
        if (!PRODUCTION_BASE_URL.equals(baseUrl)) {
            throw new IllegalStateException("KIS 실전투자 URL만 허용합니다");
        }
        if (appKey == null || appKey.isBlank() || appSecret == null || appSecret.isBlank()) {
            throw new IllegalStateException("KIS 실전투자 자격증명이 필요합니다");
        }
        if (requestTimeout == null || requestTimeout.isZero() || requestTimeout.isNegative()) {
            throw new IllegalStateException("KIS 실전투자 요청 timeout은 0보다 커야 합니다");
        }
    }

    @Override
    public String toString() {
        return "KisProductionProperties[baseUrl=" + baseUrl
                + ", appKey=***, appSecret=***, requestTimeout=" + requestTimeout + "]";
    }
}

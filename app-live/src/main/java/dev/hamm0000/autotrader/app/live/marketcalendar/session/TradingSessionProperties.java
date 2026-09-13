package dev.hamm0000.autotrader.app.live.marketcalendar.session;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("autotrader.market-calendar.session")
record TradingSessionProperties(Duration maximumAge) {

    void validate() {
        if (maximumAge == null || maximumAge.isZero() || maximumAge.isNegative()) {
            throw new IllegalStateException("거래 캘린더 최대 신뢰 기간은 0보다 커야 합니다");
        }
    }
}

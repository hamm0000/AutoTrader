package dev.hamm0000.autotrader.app.live.marketcalendar.session;

import dev.hamm0000.autotrader.trading.marketcalendar.application.port.in.session.CheckTradingSession;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.calendar.FindTradingCalendar;
import dev.hamm0000.autotrader.trading.marketcalendar.application.service.session.TradingSessionService;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.TradingSessionPolicy;
import java.time.InstantSource;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("paper-trading")
@Configuration(proxyBeanMethods = false)
class TradingSessionConfiguration {

    private static final ZoneId MARKET_ZONE = ZoneId.of("Asia/Seoul");

    @Bean
    CheckTradingSession checkTradingSession(
            FindTradingCalendar findTradingCalendar,
            TradingSessionProperties properties,
            InstantSource instantSource
    ) {
        properties.validate();
        return new TradingSessionService(
                findTradingCalendar,
                new TradingSessionPolicy(properties.maximumAge(), MARKET_ZONE),
                instantSource
        );
    }
}

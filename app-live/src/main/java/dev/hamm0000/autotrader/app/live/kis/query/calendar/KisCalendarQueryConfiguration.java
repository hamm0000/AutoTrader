package dev.hamm0000.autotrader.app.live.kis.query.calendar;

import dev.hamm0000.autotrader.adapter.kis.auth.KisAccessTokenProvider;
import dev.hamm0000.autotrader.adapter.kis.query.calendar.KisTradingDayStatusApi;
import dev.hamm0000.autotrader.app.live.kis.KisProductionProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;

@Profile("market-calendar-refresh")
@Configuration(proxyBeanMethods = false)
public class KisCalendarQueryConfiguration {

    @Bean
    KisTradingDayStatusApi kisTradingDayStatusApi(
            @Qualifier("kisProductionRestClient") RestClient restClient,
            @Qualifier("kisProductionAccessTokenProvider") KisAccessTokenProvider tokenProvider,
            KisProductionProperties properties
    ) {
        return new KisTradingDayStatusApi(
                restClient,
                tokenProvider,
                properties.appKey(),
                properties.appSecret()
        );
    }
}

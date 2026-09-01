package dev.hamm0000.autotrader.app.live.kis.query;

import dev.hamm0000.autotrader.adapter.kis.auth.KisAccessTokenProvider;
import dev.hamm0000.autotrader.adapter.kis.query.KisCurrentPriceApi;
import dev.hamm0000.autotrader.adapter.kis.query.KisDailyClosingPriceApi;
import dev.hamm0000.autotrader.app.live.kis.KisPaperProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;

@Profile("!market-calendar-refresh")
@Configuration(proxyBeanMethods = false)
public class KisPaperQueryConfiguration {

    @Bean
    KisDailyClosingPriceApi kisDailyClosingPriceApi(
            @Qualifier("kisPaperRestClient") RestClient restClient,
            @Qualifier("kisPaperAccessTokenProvider") KisAccessTokenProvider tokenProvider,
            KisPaperProperties properties
    ) {
        return new KisDailyClosingPriceApi(
                restClient,
                tokenProvider,
                properties.appKey(),
                properties.appSecret()
        );
    }

    @Bean
    KisCurrentPriceApi kisCurrentPriceApi(
            @Qualifier("kisPaperRestClient") RestClient restClient,
            @Qualifier("kisPaperAccessTokenProvider") KisAccessTokenProvider tokenProvider,
            KisPaperProperties properties
    ) {
        return new KisCurrentPriceApi(
                restClient,
                tokenProvider,
                properties.appKey(),
                properties.appSecret()
        );
    }
}

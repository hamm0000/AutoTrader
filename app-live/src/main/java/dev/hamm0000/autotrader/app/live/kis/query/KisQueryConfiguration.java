package dev.hamm0000.autotrader.app.live.kis.query;

import dev.hamm0000.autotrader.adapter.kis.auth.KisAccessTokenProvider;
import dev.hamm0000.autotrader.adapter.kis.query.KisCurrentPriceApi;
import dev.hamm0000.autotrader.adapter.kis.query.KisDailyClosingPriceApi;
import dev.hamm0000.autotrader.app.live.kis.KisProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
class KisQueryConfiguration {

    @Bean
    KisDailyClosingPriceApi kisDailyClosingPriceApi(
            @Qualifier("kisRestClient") RestClient kisRestClient,
            KisAccessTokenProvider tokenProvider,
            KisProperties properties
    ) {
        return new KisDailyClosingPriceApi(
                kisRestClient,
                tokenProvider,
                properties.appKey(),
                properties.appSecret()
        );
    }

    @Bean
    KisCurrentPriceApi kisCurrentPriceApi(
            @Qualifier("kisRestClient") RestClient kisRestClient,
            KisAccessTokenProvider tokenProvider,
            KisProperties properties
    ) {
        return new KisCurrentPriceApi(
                kisRestClient,
                tokenProvider,
                properties.appKey(),
                properties.appSecret()
        );
    }
}

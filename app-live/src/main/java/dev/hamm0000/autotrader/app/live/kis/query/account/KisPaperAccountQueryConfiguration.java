package dev.hamm0000.autotrader.app.live.kis.query.account;

import dev.hamm0000.autotrader.adapter.kis.auth.KisAccessTokenProvider;
import dev.hamm0000.autotrader.adapter.kis.query.account.KisPositionApi;
import dev.hamm0000.autotrader.app.live.kis.KisPaperProperties;
import dev.hamm0000.autotrader.app.live.kis.account.KisPaperAccountProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;

@Profile("paper-trading")
@Configuration(proxyBeanMethods = false)
public class KisPaperAccountQueryConfiguration {

    @Bean
    KisPositionApi kisPositionApi(
            @Qualifier("kisPaperRestClient") RestClient restClient,
            @Qualifier("kisPaperAccessTokenProvider") KisAccessTokenProvider tokenProvider,
            KisPaperProperties properties,
            KisPaperAccountProperties accountProperties
    ) {
        accountProperties.validate();
        return new KisPositionApi(
                restClient,
                tokenProvider,
                properties.appKey(),
                properties.appSecret(),
                accountProperties.accountNumber(),
                accountProperties.accountProductCode()
        );
    }
}

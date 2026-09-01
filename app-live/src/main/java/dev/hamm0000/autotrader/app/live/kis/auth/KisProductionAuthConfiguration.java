package dev.hamm0000.autotrader.app.live.kis.auth;

import dev.hamm0000.autotrader.adapter.kis.auth.KisAccessTokenApi;
import dev.hamm0000.autotrader.adapter.kis.auth.KisAccessTokenProvider;
import dev.hamm0000.autotrader.app.live.kis.KisProductionProperties;
import java.time.InstantSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Profile("market-calendar-refresh")
@Configuration(proxyBeanMethods = false)
class KisProductionAuthConfiguration {

    @Bean
    RestClient kisProductionRestClient(
            RestClient.Builder builder,
            KisProductionProperties properties
    ) {
        properties.validate();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(properties.requestTimeout());

        return builder.clone()
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory)
                .build();
    }

    @Bean
    InstantSource instantSource() {
        return InstantSource.system();
    }

    @Bean
    KisAccessTokenApi kisProductionAccessTokenApi(
            @Qualifier("kisProductionRestClient") RestClient restClient,
            KisProductionProperties properties,
            InstantSource instantSource
    ) {
        return new KisAccessTokenApi(
                restClient,
                properties.appKey(),
                properties.appSecret(),
                instantSource
        );
    }

    @Bean
    KisAccessTokenProvider kisProductionAccessTokenProvider(
            @Qualifier("kisProductionAccessTokenApi") KisAccessTokenApi accessTokenApi,
            InstantSource instantSource
    ) {
        return new KisAccessTokenProvider(accessTokenApi, instantSource);
    }
}

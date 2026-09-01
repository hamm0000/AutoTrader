package dev.hamm0000.autotrader.app.live.kis.auth;

import dev.hamm0000.autotrader.adapter.kis.auth.KisAccessTokenApi;
import dev.hamm0000.autotrader.adapter.kis.auth.KisAccessTokenProvider;
import dev.hamm0000.autotrader.app.live.kis.KisPaperProperties;
import java.time.InstantSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Profile("!market-calendar-refresh")
@Configuration(proxyBeanMethods = false)
class KisPaperAuthConfiguration {

    @Bean
    RestClient kisPaperRestClient(
            RestClient.Builder builder,
            KisPaperProperties properties
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
    KisAccessTokenApi kisPaperAccessTokenApi(
            @Qualifier("kisPaperRestClient") RestClient restClient,
            KisPaperProperties properties,
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
    KisAccessTokenProvider kisPaperAccessTokenProvider(
            @Qualifier("kisPaperAccessTokenApi") KisAccessTokenApi accessTokenApi,
            InstantSource instantSource
    ) {
        return new KisAccessTokenProvider(accessTokenApi, instantSource);
    }

    @Bean
    KisAccessTokenRequestRunner kisAccessTokenRequestRunner(
            @Qualifier("kisPaperAccessTokenProvider") KisAccessTokenProvider provider
    ) {
        return new KisAccessTokenRequestRunner(provider);
    }
}

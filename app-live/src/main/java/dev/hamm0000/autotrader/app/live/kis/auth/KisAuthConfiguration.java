package dev.hamm0000.autotrader.app.live.kis.auth;

import dev.hamm0000.autotrader.adapter.kis.auth.KisAccessTokenApi;
import dev.hamm0000.autotrader.adapter.kis.auth.KisAccessTokenProvider;
import java.time.InstantSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
class KisAuthConfiguration {

    @Bean
    RestClient kisRestClient(RestClient.Builder builder, KisAuthProperties properties) {
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
    KisAccessTokenApi kisAccessTokenApi(
            RestClient kisRestClient,
            KisAuthProperties properties,
            InstantSource instantSource) {
        return new KisAccessTokenApi(
                kisRestClient,
                properties.appKey(),
                properties.appSecret(),
                instantSource);
    }

    @Bean
    KisAccessTokenProvider kisAccessTokenProvider(
            KisAccessTokenApi kisAccessTokenApi,
            InstantSource instantSource) {
        return new KisAccessTokenProvider(kisAccessTokenApi, instantSource);
    }

    @Bean
    KisAccessTokenRequestRunner kisAccessTokenRequestRunner(KisAccessTokenProvider provider) {
        return new KisAccessTokenRequestRunner(provider);
    }
}

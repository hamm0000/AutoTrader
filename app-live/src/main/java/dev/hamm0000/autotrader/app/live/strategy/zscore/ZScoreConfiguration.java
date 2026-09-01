package dev.hamm0000.autotrader.app.live.strategy.zscore;

import dev.hamm0000.autotrader.adapter.strategy.zscore.http.ZScoreHttpStrategyAdapter;
import dev.hamm0000.autotrader.trading.marketdata.application.port.out.LoadCurrentPrice;
import dev.hamm0000.autotrader.trading.marketdata.application.port.out.LoadRecentClosingPrices;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.EvaluateZScore;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.EvaluateZScoreStrategy;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.service.ZScoreEvaluationService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Profile("!market-calendar-refresh")
@Configuration(proxyBeanMethods = false)
class ZScoreConfiguration {

    @Bean
    RestClient zScoreRestClient(RestClient.Builder builder, ZScoreProperties properties) {
        properties.validate();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(properties.requestTimeout()); //추후 변경

        return builder.clone()
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory)
                .build();
    }

    @Bean
    EvaluateZScoreStrategy evaluateZScoreStrategy(
            @Qualifier("zScoreRestClient") RestClient zScoreRestClient
    ) {
        return new ZScoreHttpStrategyAdapter(zScoreRestClient);
    }

    @Bean
    EvaluateZScore evaluateZScore(
            LoadRecentClosingPrices loadRecentClosingPrices,
            LoadCurrentPrice loadCurrentPrice,
            EvaluateZScoreStrategy evaluateZScoreStrategy
    ) {
        return new ZScoreEvaluationService(
                loadRecentClosingPrices,
                loadCurrentPrice,
                evaluateZScoreStrategy
        );
    }
}

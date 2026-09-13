package dev.hamm0000.autotrader.app.live.strategy.zscore;

import dev.hamm0000.autotrader.trading.marketcalendar.application.port.in.session.CheckTradingSession;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ExecuteZScoreTrade;
import java.time.InstantSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("paper-trading")
@Configuration(proxyBeanMethods = false)
class ZScoreTradingJobConfiguration {

    @Bean
    ZScoreTradingJobRunner zScoreTradingJobRunner(
            CheckTradingSession checkTradingSession,
            ExecuteZScoreTrade executeZScoreTrade,
            ZScoreTradingJobProperties properties,
            InstantSource instantSource
    ) {
        properties.validate();
        return new ZScoreTradingJobRunner(
                checkTradingSession,
                executeZScoreTrade,
                properties,
                instantSource
        );
    }
}

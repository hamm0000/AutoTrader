package dev.hamm0000.autotrader.app.live.persistence;

import dev.hamm0000.autotrader.adapter.postgres.order.OrderPersistenceAdapter;
import dev.hamm0000.autotrader.adapter.postgres.strategy.zscore.ZScoreTradePlanPersistenceAdapter;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Profile("paper-trading")
@Configuration(proxyBeanMethods = false)
@EntityScan(basePackages = {
        "dev.hamm0000.autotrader.adapter.postgres.order",
        "dev.hamm0000.autotrader.adapter.postgres.strategy.zscore"
})
@EnableJpaRepositories(basePackages = {
        "dev.hamm0000.autotrader.adapter.postgres.order",
        "dev.hamm0000.autotrader.adapter.postgres.strategy.zscore"
})
@Import({
        OrderPersistenceAdapter.class,
        ZScoreTradePlanPersistenceAdapter.class
})
class PaperTradingPersistenceConfiguration {
}

package dev.hamm0000.autotrader.adapter.postgres.marketcalendar;

import dev.hamm0000.autotrader.adapter.postgres.marketcalendar.calendar.TradingCalendarPersistenceAdapter;
import dev.hamm0000.autotrader.adapter.postgres.marketcalendar.specialsession.MarketSessionOverridePersistenceAdapter;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan("dev.hamm0000.autotrader.adapter.postgres.marketcalendar")
@EnableJpaRepositories("dev.hamm0000.autotrader.adapter.postgres.marketcalendar")
@Import({
        TradingCalendarPersistenceAdapter.class,
        MarketSessionOverridePersistenceAdapter.class
})
public class PostgresAdapterTestApplication {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:17.11-alpine");
    }
}

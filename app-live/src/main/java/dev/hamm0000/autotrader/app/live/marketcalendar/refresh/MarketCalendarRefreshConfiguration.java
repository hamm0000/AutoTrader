package dev.hamm0000.autotrader.app.live.marketcalendar.refresh;

import dev.hamm0000.autotrader.adapter.postgres.marketcalendar.calendar.TradingCalendarPersistenceAdapter;
import dev.hamm0000.autotrader.adapter.postgres.marketcalendar.specialsession.MarketSessionOverridePersistenceAdapter;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.in.refresh.RefreshTradingCalendar;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.calendar.SaveTradingCalendar;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.specialsession.FindMarketSessionOverrides;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.status.LoadTradingDayStatuses;
import dev.hamm0000.autotrader.trading.marketcalendar.application.service.refresh.TradingCalendarRefreshService;
import java.time.InstantSource;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Profile("market-calendar-refresh")
@Configuration(proxyBeanMethods = false)
@EntityScan("dev.hamm0000.autotrader.adapter.postgres.marketcalendar")
@EnableJpaRepositories("dev.hamm0000.autotrader.adapter.postgres.marketcalendar")
@Import({
        TradingCalendarPersistenceAdapter.class,
        MarketSessionOverridePersistenceAdapter.class
})
class MarketCalendarRefreshConfiguration {

    @Bean
    RefreshTradingCalendar refreshTradingCalendar(
            LoadTradingDayStatuses loadTradingDayStatuses,
            FindMarketSessionOverrides findMarketSessionOverrides,
            SaveTradingCalendar saveTradingCalendar,
            InstantSource instantSource
    ) {
        return new TradingCalendarRefreshService(
                loadTradingDayStatuses,
                findMarketSessionOverrides,
                saveTradingCalendar,
                instantSource
        );
    }

    @Bean
    MarketCalendarRefreshRunner marketCalendarRefreshRunner(
            RefreshTradingCalendar refreshTradingCalendar,
            MarketCalendarRefreshProperties properties,
            InstantSource instantSource
    ) {
        return new MarketCalendarRefreshRunner(
                refreshTradingCalendar,
                properties,
                instantSource
        );
    }
}

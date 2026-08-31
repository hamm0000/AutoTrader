package dev.hamm0000.autotrader.adapter.postgres.marketcalendar.specialsession;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface MarketSessionOverrideRepository
        extends JpaRepository<MarketSessionOverrideEntity, LocalDate> {

    List<MarketSessionOverrideEntity> findByTradeDateBetweenOrderByTradeDateAsc(
            LocalDate from,
            LocalDate through
    );
}

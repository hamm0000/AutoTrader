package dev.hamm0000.autotrader.adapter.postgres.marketcalendar.calendar;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface TradingDayRepository extends JpaRepository<TradingDayEntity, LocalDate> {

    List<TradingDayEntity> findByTradeDateBetweenOrderByTradeDateAsc(
            LocalDate from,
            LocalDate through
    );
}

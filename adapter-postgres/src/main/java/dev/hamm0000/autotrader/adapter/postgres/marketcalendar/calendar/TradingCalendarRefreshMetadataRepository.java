package dev.hamm0000.autotrader.adapter.postgres.marketcalendar.calendar;

import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

interface TradingCalendarRefreshMetadataRepository
        extends JpaRepository<TradingCalendarRefreshMetadataEntity, LocalDate> {
}

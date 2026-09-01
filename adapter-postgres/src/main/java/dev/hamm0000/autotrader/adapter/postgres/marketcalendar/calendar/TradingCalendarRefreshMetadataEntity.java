package dev.hamm0000.autotrader.adapter.postgres.marketcalendar.calendar;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "trading_calendar_refresh_metadata")
public class TradingCalendarRefreshMetadataEntity {

    @Id
    @Column(name = "coverage_from", nullable = false)
    private LocalDate coverageFrom;

    @Column(name = "coverage_through", nullable = false)
    private LocalDate coverageThrough;

    @Column(name = "last_successful_refresh_at", nullable = false)
    private Instant lastSuccessfulRefreshAt;

    protected TradingCalendarRefreshMetadataEntity() {
    }

    TradingCalendarRefreshMetadataEntity(
            LocalDate coverageFrom,
            LocalDate coverageThrough,
            Instant lastSuccessfulRefreshAt
    ) {
        this.coverageFrom = coverageFrom;
        this.coverageThrough = coverageThrough;
        this.lastSuccessfulRefreshAt = lastSuccessfulRefreshAt;
    }
}

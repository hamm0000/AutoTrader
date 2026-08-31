package dev.hamm0000.autotrader.adapter.postgres.marketcalendar.calendar;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "trading_calendar")
public class TradingDayEntity {

    @Id
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "is_trading_day", nullable = false)
    private boolean tradingDay;

    @Column(name = "session_open")
    private LocalTime sessionOpen;

    @Column(name = "session_close")
    private LocalTime sessionClose;

    @Column(name = "note")
    private String note;

    @Column(name = "verified_at", nullable = false)
    private Instant verifiedAt;

    protected TradingDayEntity() {
    }

    TradingDayEntity(
            LocalDate tradeDate,
            boolean tradingDay,
            LocalTime sessionOpen,
            LocalTime sessionClose,
            String note,
            Instant verifiedAt
    ) {
        this.tradeDate = tradeDate;
        this.tradingDay = tradingDay;
        this.sessionOpen = sessionOpen;
        this.sessionClose = sessionClose;
        this.note = note;
        this.verifiedAt = verifiedAt;
    }

    LocalDate tradeDate() {
        return tradeDate;
    }

    boolean tradingDay() {
        return tradingDay;
    }

    LocalTime sessionOpen() {
        return sessionOpen;
    }

    LocalTime sessionClose() {
        return sessionClose;
    }

    String note() {
        return note;
    }

    Instant verifiedAt() {
        return verifiedAt;
    }
}

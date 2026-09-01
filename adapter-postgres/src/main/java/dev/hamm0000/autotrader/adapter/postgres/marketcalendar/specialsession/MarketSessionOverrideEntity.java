package dev.hamm0000.autotrader.adapter.postgres.marketcalendar.specialsession;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "market_session_override")
public class MarketSessionOverrideEntity {

    @Id
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "session_open", nullable = false)
    private LocalTime sessionOpen;

    @Column(name = "session_close", nullable = false)
    private LocalTime sessionClose;

    @Column(name = "note", nullable = false)
    private String note;

    protected MarketSessionOverrideEntity() {
    }

    LocalDate tradeDate() {
        return tradeDate;
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
}

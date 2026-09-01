package dev.hamm0000.autotrader.trading.marketcalendar.application.service.refresh.testdouble;

import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.calendar.SaveTradingCalendar;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.TradingCalendar;
import java.util.Objects;
import java.util.Optional;

public final class FakeTradingCalendarSaver implements SaveTradingCalendar {

    private TradingCalendar savedCalendar;
    private RuntimeException failure;
    private int saveCount;

    @Override
    public void save(TradingCalendar calendar) {
        Objects.requireNonNull(calendar, "calendar must not be null");
        if (failure != null) {
            throw failure;
        }
        savedCalendar = calendar;
        saveCount++;
    }

    public void failWith(RuntimeException failure) {
        this.failure = Objects.requireNonNull(failure, "failure must not be null");
    }

    public Optional<TradingCalendar> savedCalendar() {
        return Optional.ofNullable(savedCalendar);
    }

    public int saveCount() {
        return saveCount;
    }
}

package dev.hamm0000.autotrader.trading.marketcalendar.application.service.session.testdouble;

import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.calendar.FindTradingCalendar;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.calendar.TradingCalendarQueryException;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.CalendarPeriod;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.TradingCalendar;
import java.util.Objects;
import java.util.Optional;

public final class StubTradingCalendarFinder implements FindTradingCalendar {

    private final Optional<TradingCalendar> calendar;
    private CalendarPeriod period;
    private TradingCalendarQueryException failure;

    public StubTradingCalendarFinder(Optional<TradingCalendar> calendar) {
        this.calendar = Objects.requireNonNull(calendar, "calendar must not be null");
    }

    @Override
    public Optional<TradingCalendar> find(CalendarPeriod period) {
        this.period = Objects.requireNonNull(period, "period must not be null");
        if (failure != null) {
            throw failure;
        }
        return calendar;
    }

    public void failWith(TradingCalendarQueryException failure) {
        this.failure = Objects.requireNonNull(failure, "failure must not be null");
    }

    public CalendarPeriod period() {
        return period;
    }
}

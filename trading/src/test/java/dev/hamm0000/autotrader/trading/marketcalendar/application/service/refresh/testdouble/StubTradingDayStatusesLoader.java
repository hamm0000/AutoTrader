package dev.hamm0000.autotrader.trading.marketcalendar.application.service.refresh.testdouble;

import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.status.LoadTradingDayStatuses;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.status.TradingDayStatuses;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.CalendarPeriod;
import java.util.Objects;

public final class StubTradingDayStatusesLoader implements LoadTradingDayStatuses {

    private final TradingDayStatuses statuses;
    private CalendarPeriod period;
    private RuntimeException failure;

    public StubTradingDayStatusesLoader(TradingDayStatuses statuses) {
        this.statuses = Objects.requireNonNull(statuses, "statuses must not be null");
    }

    @Override
    public TradingDayStatuses load(CalendarPeriod period) {
        this.period = Objects.requireNonNull(period, "period must not be null");
        if (failure != null) {
            throw failure;
        }
        return statuses;
    }

    public void failWith(RuntimeException failure) {
        this.failure = Objects.requireNonNull(failure, "failure must not be null");
    }

    public CalendarPeriod period() {
        return period;
    }
}

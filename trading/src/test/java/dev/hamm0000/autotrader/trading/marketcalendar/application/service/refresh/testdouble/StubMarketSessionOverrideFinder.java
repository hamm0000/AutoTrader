package dev.hamm0000.autotrader.trading.marketcalendar.application.service.refresh.testdouble;

import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.specialsession.FindMarketSessionOverrides;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.CalendarPeriod;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.MarketSessionOverride;
import java.util.List;
import java.util.Objects;

public final class StubMarketSessionOverrideFinder implements FindMarketSessionOverrides {

    private final List<MarketSessionOverride> overrides;
    private CalendarPeriod period;
    private RuntimeException failure;

    public StubMarketSessionOverrideFinder(List<MarketSessionOverride> overrides) {
        Objects.requireNonNull(overrides, "overrides must not be null");
        this.overrides = List.copyOf(overrides);
    }

    @Override
    public List<MarketSessionOverride> find(CalendarPeriod period) {
        this.period = Objects.requireNonNull(period, "period must not be null");
        if (failure != null) {
            throw failure;
        }
        return overrides;
    }

    public void failWith(RuntimeException failure) {
        this.failure = Objects.requireNonNull(failure, "failure must not be null");
    }

    public CalendarPeriod period() {
        return period;
    }
}

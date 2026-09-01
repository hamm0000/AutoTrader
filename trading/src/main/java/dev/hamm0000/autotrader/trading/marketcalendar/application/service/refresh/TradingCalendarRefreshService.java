package dev.hamm0000.autotrader.trading.marketcalendar.application.service.refresh;

import dev.hamm0000.autotrader.trading.marketcalendar.application.port.in.refresh.RefreshTradingCalendar;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.in.refresh.TradingCalendarRefreshCommand;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.calendar.SaveTradingCalendar;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.specialsession.FindMarketSessionOverrides;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.status.LoadTradingDayStatuses;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.status.TradingDayStatuses;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.CalendarPeriod;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.MarketSessionOverride;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.MarketSessionSchedule;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.TradingCalendar;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.TradingDay;
import java.time.Instant;
import java.time.InstantSource;
import java.util.List;
import java.util.Objects;

public final class TradingCalendarRefreshService implements RefreshTradingCalendar {

    private final LoadTradingDayStatuses loadTradingDayStatuses;
    private final FindMarketSessionOverrides findMarketSessionOverrides;
    private final SaveTradingCalendar saveTradingCalendar;
    private final InstantSource timeSource;

    public TradingCalendarRefreshService(
            LoadTradingDayStatuses loadTradingDayStatuses,
            FindMarketSessionOverrides findMarketSessionOverrides,
            SaveTradingCalendar saveTradingCalendar,
            InstantSource timeSource
    ) {
        this.loadTradingDayStatuses = Objects.requireNonNull(
                loadTradingDayStatuses,
                "loadTradingDayStatuses must not be null"
        );
        this.findMarketSessionOverrides = Objects.requireNonNull(
                findMarketSessionOverrides,
                "findMarketSessionOverrides must not be null"
        );
        this.saveTradingCalendar = Objects.requireNonNull(
                saveTradingCalendar,
                "saveTradingCalendar must not be null"
        );
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource must not be null");
    }

    @Override
    public void refresh(TradingCalendarRefreshCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        CalendarPeriod requestedPeriod = command.period();

        TradingDayStatuses statuses = Objects.requireNonNull(
                loadTradingDayStatuses.load(requestedPeriod),
                "loaded trading day statuses must not be null"
        );
        requireRequestedCoverage(requestedPeriod, statuses.coverage());

        List<MarketSessionOverride> overrides = Objects.requireNonNull(
                findMarketSessionOverrides.find(requestedPeriod),
                "market session overrides must not be null"
        );
        MarketSessionSchedule schedule = new MarketSessionSchedule(overrides);
        Instant verifiedAt = timeSource.instant();

        List<TradingDay> tradingDays = statuses.values().stream()
                .map(status -> schedule.createTradingDay(
                        status.date(),
                        status.status(),
                        verifiedAt
                ))
                .toList();

        saveTradingCalendar.save(new TradingCalendar(requestedPeriod, tradingDays));
    }

    private static void requireRequestedCoverage(
            CalendarPeriod requested,
            CalendarPeriod loaded
    ) {
        if (!requested.equals(loaded)) {
            throw new IllegalStateException(
                    "loaded trading day statuses must match the requested period"
            );
        }
    }
}

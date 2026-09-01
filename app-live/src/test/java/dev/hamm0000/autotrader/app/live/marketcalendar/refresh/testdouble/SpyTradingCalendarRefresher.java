package dev.hamm0000.autotrader.app.live.marketcalendar.refresh.testdouble;

import dev.hamm0000.autotrader.trading.marketcalendar.application.port.in.refresh.RefreshTradingCalendar;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.in.refresh.TradingCalendarRefreshCommand;
import java.util.Objects;
import java.util.Optional;

public final class SpyTradingCalendarRefresher implements RefreshTradingCalendar {

    private TradingCalendarRefreshCommand command;
    private RuntimeException failure;
    private int refreshCount;

    @Override
    public void refresh(TradingCalendarRefreshCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        if (failure != null) {
            throw failure;
        }
        this.command = command;
        refreshCount++;
    }

    public void failWith(RuntimeException failure) {
        this.failure = Objects.requireNonNull(failure, "failure must not be null");
    }

    public Optional<TradingCalendarRefreshCommand> command() {
        return Optional.ofNullable(command);
    }

    public int refreshCount() {
        return refreshCount;
    }
}

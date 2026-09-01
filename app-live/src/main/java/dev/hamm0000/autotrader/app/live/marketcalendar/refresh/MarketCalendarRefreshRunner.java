package dev.hamm0000.autotrader.app.live.marketcalendar.refresh;

import dev.hamm0000.autotrader.trading.marketcalendar.application.port.in.refresh.RefreshTradingCalendar;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.in.refresh.TradingCalendarRefreshCommand;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.CalendarPeriod;
import java.time.InstantSource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;


// 실패를 main까지 전파해 외부에서 이 실행의 실패를 감지할 수 있게 함
final class MarketCalendarRefreshRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MarketCalendarRefreshRunner.class);
    private static final ZoneId MARKET_ZONE = ZoneId.of("Asia/Seoul");

    private final RefreshTradingCalendar refreshTradingCalendar;
    private final MarketCalendarRefreshProperties properties;
    private final InstantSource timeSource;

    MarketCalendarRefreshRunner(
            RefreshTradingCalendar refreshTradingCalendar,
            MarketCalendarRefreshProperties properties,
            InstantSource timeSource
    ) {
        this.refreshTradingCalendar = Objects.requireNonNull(
                refreshTradingCalendar,
                "refreshTradingCalendar must not be null"
        );
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource must not be null");
    }

    @Override
    public void run(ApplicationArguments arguments) {
        LocalDate startsOn = LocalDate.ofInstant(timeSource.instant(), MARKET_ZONE);
        CalendarPeriod period = properties.periodStartingOn(startsOn);

        refreshTradingCalendar.refresh(new TradingCalendarRefreshCommand(period));
        log.info("거래 캘린더 갱신 완료 (from={}, through={})", period.from(), period.through());
    }
}

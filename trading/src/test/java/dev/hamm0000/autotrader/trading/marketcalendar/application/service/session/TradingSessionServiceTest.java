package dev.hamm0000.autotrader.trading.marketcalendar.application.service.session;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.hamm0000.autotrader.trading.marketcalendar.application.port.in.session.TradingSessionCheckCommand;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.calendar.TradingCalendarQueryException;
import dev.hamm0000.autotrader.trading.marketcalendar.application.service.session.testdouble.StubTradingCalendarFinder;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.CalendarPeriod;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.MarketSession;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.TradingCalendar;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.TradingDay;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.TradingSessionDecision;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.TradingSessionPolicy;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TradingSessionServiceTest {

    private static final LocalDate MARKET_DATE = LocalDate.of(2026, 8, 17);
    private static final Instant NOW = Instant.parse("2026-08-17T03:00:00Z");
    private static final TradingCalendar CALENDAR = new TradingCalendar(
            new CalendarPeriod(MARKET_DATE, MARKET_DATE),
            List.of(TradingDay.open(
                    MARKET_DATE,
                    new MarketSession(LocalTime.of(9, 0), LocalTime.of(15, 30)),
                    NOW
            ))
    );

    @Test
    void 오늘이_거래일인지_저장된_캘린더로_판단한다() {
        // given
        StubTradingCalendarFinder finder = new StubTradingCalendarFinder(
                Optional.of(CALENDAR)
        );
        TradingSessionService service = service(finder);

        // when
        TradingSessionDecision decision = service.check(command());

        // then
        assertAll(
                () -> assertEquals(TradingSessionDecision.start(), decision),
                () -> assertEquals(
                        new CalendarPeriod(MARKET_DATE, MARKET_DATE),
                        finder.period()
                )
        );
    }

    @Test
    void 오늘의_캘린더가_없으면_degraded_상태로_거래_준비를_계속한다() {
        // given
        StubTradingCalendarFinder finder = new StubTradingCalendarFinder(Optional.empty());

        // when
        TradingSessionDecision decision = service(finder).check(command());

        // then
        assertEquals(
                TradingSessionDecision.startDegraded(
                        TradingSessionDecision.Reason.CALENDAR_MISSING
                ),
                decision
        );
    }

    @Test
    void 저장된_캘린더를_조회할_수_없어도_degraded_상태로_거래_준비를_계속한다() {
        // given
        StubTradingCalendarFinder finder = new StubTradingCalendarFinder(Optional.empty());
        finder.failWith(new TradingCalendarQueryException(
                "거래 캘린더 저장소를 사용할 수 없습니다"
        ));

        // when
        TradingSessionDecision decision = service(finder).check(command());

        // then
        assertEquals(
                TradingSessionDecision.startDegraded(
                        TradingSessionDecision.Reason.CALENDAR_UNAVAILABLE
                ),
                decision
        );
    }

    private static TradingSessionService service(StubTradingCalendarFinder finder) {
        return new TradingSessionService(
                finder,
                new TradingSessionPolicy(Duration.ofDays(7), ZoneId.of("Asia/Seoul")),
                InstantSource.fixed(NOW)
        );
    }

    private static TradingSessionCheckCommand command() {
        return new TradingSessionCheckCommand(MARKET_DATE);
    }
}

package dev.hamm0000.autotrader.trading.marketcalendar.application.service.refresh;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.hamm0000.autotrader.trading.marketcalendar.application.port.in.refresh.TradingCalendarRefreshCommand;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.calendar.TradingCalendarSaveException;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.specialsession.MarketSessionOverrideQueryException;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.status.TradingDayStatusObservation;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.status.TradingDayStatusQueryException;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.status.TradingDayStatuses;
import dev.hamm0000.autotrader.trading.marketcalendar.application.service.refresh.testdouble.FakeTradingCalendarSaver;
import dev.hamm0000.autotrader.trading.marketcalendar.application.service.refresh.testdouble.StubMarketSessionOverrideFinder;
import dev.hamm0000.autotrader.trading.marketcalendar.application.service.refresh.testdouble.StubTradingDayStatusesLoader;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.CalendarPeriod;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.MarketSession;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.MarketSessionOverride;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.TradingCalendar;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.TradingDay;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.TradingDayStatus;
import java.time.Instant;
import java.time.InstantSource;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class TradingCalendarRefreshServiceTest {

    private static final CalendarPeriod PERIOD = new CalendarPeriod(
            LocalDate.of(2026, 11, 11),
            LocalDate.of(2026, 11, 13)
    );
    private static final Instant VERIFIED_AT = Instant.parse("2026-11-11T07:00:00Z");
    private static final MarketSession SPECIAL_SESSION = new MarketSession(
            LocalTime.of(10, 0),
            LocalTime.of(16, 30)
    );

    @Test
    void 개장_상태와_날짜별_세션을_병합해_완전한_캘린더를_한_번_저장한다() {
        // given
        StubTradingDayStatusesLoader statuses = new StubTradingDayStatusesLoader(statuses());
        StubMarketSessionOverrideFinder overrides = new StubMarketSessionOverrideFinder(List.of(
                new MarketSessionOverride(
                        LocalDate.of(2026, 11, 12),
                        SPECIAL_SESSION,
                        "수능"
                )
        ));
        FakeTradingCalendarSaver calendars = new FakeTradingCalendarSaver();
        AdvancingInstantSource timeSource = new AdvancingInstantSource(VERIFIED_AT);
        TradingCalendarRefreshService service = service(
                statuses,
                overrides,
                calendars,
                timeSource
        );

        // when
        service.refresh(new TradingCalendarRefreshCommand(PERIOD));

        // then
        TradingCalendar saved = calendars.savedCalendar().orElseThrow();
        assertAll(
                () -> assertEquals(PERIOD, statuses.period()),
                () -> assertEquals(PERIOD, overrides.period()),
                () -> assertEquals(PERIOD, saved.coverage()),
                () -> assertEquals(1, calendars.saveCount()),
                () -> assertEquals(1, timeSource.callCount()),
                () -> assertEquals(
                        TradingDay.open(
                                LocalDate.of(2026, 11, 11),
                                new MarketSession(LocalTime.of(9, 0), LocalTime.of(15, 30)),
                                VERIFIED_AT
                        ),
                        saved.find(LocalDate.of(2026, 11, 11)).orElseThrow()
                ),
                () -> assertEquals(
                        TradingDay.open(
                                LocalDate.of(2026, 11, 12),
                                SPECIAL_SESSION,
                                "수능",
                                VERIFIED_AT
                        ),
                        saved.find(LocalDate.of(2026, 11, 12)).orElseThrow()
                ),
                () -> assertEquals(
                        TradingDay.closed(LocalDate.of(2026, 11, 13), VERIFIED_AT),
                        saved.find(LocalDate.of(2026, 11, 13)).orElseThrow()
                )
        );
    }

    @Test
    void 개장_상태를_조회하지_못하면_기존_캘린더를_덮어쓰지_않는다() {
        // given
        StubTradingDayStatusesLoader statuses = new StubTradingDayStatusesLoader(statuses());
        TradingDayStatusQueryException expected =
                TradingDayStatusQueryException.providerUnavailable("개장 상태 조회 실패");
        statuses.failWith(expected);
        FakeTradingCalendarSaver calendars = new FakeTradingCalendarSaver();

        // when
        TradingDayStatusQueryException actual = assertThrows(
                TradingDayStatusQueryException.class,
                () -> service(
                        statuses,
                        new StubMarketSessionOverrideFinder(List.of()),
                        calendars,
                        InstantSource.fixed(VERIFIED_AT)
                ).refresh(new TradingCalendarRefreshCommand(PERIOD))
        );

        // then
        assertAll(
                () -> assertSame(expected, actual),
                () -> assertEquals(0, calendars.saveCount())
        );
    }

    @Test
    void 날짜별_세션을_조회하지_못하면_기존_캘린더를_덮어쓰지_않는다() {
        // given
        StubMarketSessionOverrideFinder overrides = new StubMarketSessionOverrideFinder(List.of());
        MarketSessionOverrideQueryException expected = new MarketSessionOverrideQueryException(
                "날짜별 세션 조회 실패"
        );
        overrides.failWith(expected);
        FakeTradingCalendarSaver calendars = new FakeTradingCalendarSaver();

        // when
        MarketSessionOverrideQueryException actual = assertThrows(
                MarketSessionOverrideQueryException.class,
                () -> service(
                        new StubTradingDayStatusesLoader(statuses()),
                        overrides,
                        calendars,
                        InstantSource.fixed(VERIFIED_AT)
                ).refresh(new TradingCalendarRefreshCommand(PERIOD))
        );

        // then
        assertAll(
                () -> assertSame(expected, actual),
                () -> assertEquals(0, calendars.saveCount())
        );
    }

    @Test
    void 요청과_다른_범위의_개장_상태가_반환되면_저장하지_않는다() {
        // given
        CalendarPeriod unexpectedPeriod = new CalendarPeriod(
                PERIOD.from(),
                PERIOD.through().minusDays(1)
        );
        FakeTradingCalendarSaver calendars = new FakeTradingCalendarSaver();

        // when
        assertThrows(
                IllegalStateException.class,
                () -> service(
                        new StubTradingDayStatusesLoader(new TradingDayStatuses(
                                unexpectedPeriod,
                                List.of(
                                        new TradingDayStatusObservation(
                                                LocalDate.of(2026, 11, 11),
                                                TradingDayStatus.OPEN
                                        ),
                                        new TradingDayStatusObservation(
                                                LocalDate.of(2026, 11, 12),
                                                TradingDayStatus.OPEN
                                        )
                                )
                        )),
                        new StubMarketSessionOverrideFinder(List.of()),
                        calendars,
                        InstantSource.fixed(VERIFIED_AT)
                ).refresh(new TradingCalendarRefreshCommand(PERIOD))
        );

        // then
        assertEquals(0, calendars.saveCount());
    }

    @Test
    void 완성된_캘린더를_저장하지_못하면_실패를_호출자에게_전달한다() {
        // given
        FakeTradingCalendarSaver calendars = new FakeTradingCalendarSaver();
        TradingCalendarSaveException expected = new TradingCalendarSaveException(
                "거래 캘린더 저장 실패"
        );
        calendars.failWith(expected);

        // when
        TradingCalendarSaveException actual = assertThrows(
                TradingCalendarSaveException.class,
                () -> service(
                        new StubTradingDayStatusesLoader(statuses()),
                        new StubMarketSessionOverrideFinder(List.of()),
                        calendars,
                        InstantSource.fixed(VERIFIED_AT)
                ).refresh(new TradingCalendarRefreshCommand(PERIOD))
        );

        // then
        assertSame(expected, actual);
    }

    private static TradingCalendarRefreshService service(
            StubTradingDayStatusesLoader statuses,
            StubMarketSessionOverrideFinder overrides,
            FakeTradingCalendarSaver calendars,
            InstantSource timeSource
    ) {
        return new TradingCalendarRefreshService(
                statuses,
                overrides,
                calendars,
                timeSource
        );
    }

    private static TradingDayStatuses statuses() {
        return new TradingDayStatuses(
                PERIOD,
                List.of(
                        new TradingDayStatusObservation(
                                LocalDate.of(2026, 11, 11),
                                TradingDayStatus.OPEN
                        ),
                        new TradingDayStatusObservation(
                                LocalDate.of(2026, 11, 12),
                                TradingDayStatus.OPEN
                        ),
                        new TradingDayStatusObservation(
                                LocalDate.of(2026, 11, 13),
                                TradingDayStatus.CLOSED
                        )
                )
        );
    }

    private static final class AdvancingInstantSource implements InstantSource {

        private Instant next;
        private int callCount;

        private AdvancingInstantSource(Instant first) {
            this.next = first;
        }

        @Override
        public Instant instant() {
            Instant current = next;
            next = next.plusSeconds(1);
            callCount++;
            return current;
        }

        private int callCount() {
            return callCount;
        }
    }
}

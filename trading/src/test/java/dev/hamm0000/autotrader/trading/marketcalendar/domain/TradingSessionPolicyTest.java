package dev.hamm0000.autotrader.trading.marketcalendar.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class TradingSessionPolicyTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 17);
    private static final Instant NOW = Instant.parse("2026-08-17T08:25:00Z");
    private static final Duration MAXIMUM_CALENDAR_AGE = Duration.ofDays(7);
    private static final MarketSession SESSION = new MarketSession(
            LocalTime.of(9, 0),
            LocalTime.of(15, 30)
    );
    private static final TradingSessionPolicy POLICY =
            new TradingSessionPolicy(MAXIMUM_CALENDAR_AGE);

    @Test
    void 정상_확인한_거래일이면_거래_준비를_계속한다() {
        // given
        TradingCalendar calendar = calendar(TradingDay.open(DATE, SESSION, NOW));

        // when
        TradingSessionDecision decision = POLICY.decide(calendar, DATE, NOW);

        // then
        assertEquals(TradingSessionDecision.start(), decision);
    }

    @Test
    void 휴장일이면_런타임을_정상_종료한다() {
        // given
        TradingCalendar calendar = calendar(TradingDay.closed(DATE, NOW));

        // when
        TradingSessionDecision decision = POLICY.decide(calendar, DATE, NOW);

        // then
        assertEquals(TradingSessionDecision.stopClosed(), decision);
    }

    @Test
    void 거래일_정보가_없으면_degraded_상태로_거래_준비를_계속한다() {
        // given
        TradingCalendar calendar = new TradingCalendar(
                new CalendarPeriod(DATE.minusDays(1), DATE.minusDays(1)),
                List.of(TradingDay.open(DATE.minusDays(1), SESSION, NOW))
        );

        // when
        TradingSessionDecision decision = POLICY.decide(calendar, DATE, NOW);

        // then
        assertEquals(
                TradingSessionDecision.startDegraded(
                        TradingSessionDecision.Reason.CALENDAR_MISSING
                ),
                decision
        );
    }

    @Test
    void 정상_확인_후_정확히_7일까지는_거래일_정보를_사용한다() {
        // given
        TradingCalendar calendar = calendar(TradingDay.open(
                DATE,
                SESSION,
                NOW.minus(MAXIMUM_CALENDAR_AGE)
        ));

        // when
        TradingSessionDecision decision = POLICY.decide(calendar, DATE, NOW);

        // then
        assertEquals(TradingSessionDecision.start(), decision);
    }

    @Test
    void 정상_확인_후_7일을_넘긴_거래일은_degraded_상태로_준비한다() {
        // given
        TradingCalendar calendar = calendar(TradingDay.open(
                DATE,
                SESSION,
                NOW.minus(MAXIMUM_CALENDAR_AGE).minusSeconds(1)
        ));

        // when
        TradingSessionDecision decision = POLICY.decide(calendar, DATE, NOW);

        // then
        assertEquals(
                TradingSessionDecision.startDegraded(
                        TradingSessionDecision.Reason.CALENDAR_STALE
                ),
                decision
        );
    }

    @Test
    void 정상_확인_후_7일을_넘긴_휴장일도_degraded_상태로_준비한다() {
        // given
        TradingCalendar calendar = calendar(TradingDay.closed(
                DATE,
                NOW.minus(MAXIMUM_CALENDAR_AGE).minusSeconds(1)
        ));

        // when
        TradingSessionDecision decision = POLICY.decide(calendar, DATE, NOW);

        // then
        assertEquals(
                TradingSessionDecision.startDegraded(
                        TradingSessionDecision.Reason.CALENDAR_STALE
                ),
                decision
        );
    }

    @Test
    void 거래일_정보의_최대_신뢰_기간은_양수여야_한다() {
        // when & then
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new TradingSessionPolicy(Duration.ZERO)
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new TradingSessionPolicy(Duration.ofSeconds(-1))
                )
        );
    }

    private static TradingCalendar calendar(TradingDay day) {
        return new TradingCalendar(new CalendarPeriod(DATE, DATE), List.of(day));
    }
}

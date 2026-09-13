package dev.hamm0000.autotrader.trading.marketcalendar.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class TradingSessionPolicyTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 17);
    private static final ZoneId MARKET_ZONE = ZoneId.of("Asia/Seoul");
    private static final Instant NOW = Instant.parse("2026-08-17T03:00:00Z");
    private static final Duration MAXIMUM_CALENDAR_AGE = Duration.ofDays(7);
    private static final MarketSession SESSION = new MarketSession(
            LocalTime.of(9, 0),
            LocalTime.of(15, 30)
    );
    private static final TradingSessionPolicy POLICY =
            new TradingSessionPolicy(MAXIMUM_CALENDAR_AGE, MARKET_ZONE);

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
    void 장_시작_시각부터_거래를_시작한다() {
        // given
        Instant sessionOpen = Instant.parse("2026-08-17T00:00:00Z");
        TradingCalendar calendar = calendar(TradingDay.open(DATE, SESSION, sessionOpen));

        // when
        TradingSessionDecision decision = POLICY.decide(calendar, DATE, sessionOpen);

        // then
        assertEquals(TradingSessionDecision.start(), decision);
    }

    @Test
    void 장_시작_전에는_거래를_시작하지_않는다() {
        // given
        Instant beforeSession = Instant.parse("2026-08-16T23:59:59Z");
        TradingCalendar calendar = calendar(TradingDay.open(DATE, SESSION, beforeSession));

        // when
        TradingSessionDecision decision = POLICY.decide(calendar, DATE, beforeSession);

        // then
        assertEquals(TradingSessionDecision.stopOutsideSession(), decision);
    }

    @Test
    void 장_종료_시각부터_거래를_시작하지_않는다() {
        // given
        Instant sessionClose = Instant.parse("2026-08-17T06:30:00Z");
        TradingCalendar calendar = calendar(TradingDay.open(DATE, SESSION, sessionClose));

        // when
        TradingSessionDecision decision = POLICY.decide(calendar, DATE, sessionClose);

        // then
        assertEquals(TradingSessionDecision.stopOutsideSession(), decision);
    }

    @Test
    void 평가_순간의_한국_거래일이_요청일과_다르면_거래를_시작하지_않는다() {
        // given
        TradingCalendar calendar = calendar(TradingDay.open(DATE, SESSION, NOW));
        Instant nextMarketDate = Instant.parse("2026-08-18T03:00:00Z");

        // when
        TradingSessionDecision decision = POLICY.decide(calendar, DATE, nextMarketDate);

        // then
        assertEquals(TradingSessionDecision.stopOutsideSession(), decision);
    }

    @Test
    void 거래일_정보의_최대_신뢰_기간은_양수여야_한다() {
        // when & then
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new TradingSessionPolicy(Duration.ZERO, MARKET_ZONE)
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new TradingSessionPolicy(Duration.ofSeconds(-1), MARKET_ZONE)
                )
        );
    }

    private static TradingCalendar calendar(TradingDay day) {
        return new TradingCalendar(new CalendarPeriod(DATE, DATE), List.of(day));
    }
}

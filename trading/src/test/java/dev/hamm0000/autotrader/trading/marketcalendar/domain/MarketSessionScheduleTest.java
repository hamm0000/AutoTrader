package dev.hamm0000.autotrader.trading.marketcalendar.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarketSessionScheduleTest {

    private static final LocalDate MARKET_DATE = LocalDate.of(2026, 11, 12);
    private static final Instant VERIFIED_AT = Instant.parse("2026-11-11T07:00:00Z");
    private static final MarketSession SPECIAL_SESSION = new MarketSession(
            LocalTime.of(10, 0),
            LocalTime.of(16, 30)
    );

    @Test
    void 예외_일정이_없는_개장일에는_정규_세션을_적용한다() {
        // given
        MarketSessionSchedule schedule = new MarketSessionSchedule(List.of());

        // when
        TradingDay tradingDay = schedule.createTradingDay(
                MARKET_DATE,
                TradingDayStatus.OPEN,
                VERIFIED_AT
        );

        // then
        assertEquals(
                TradingDay.open(
                        MARKET_DATE,
                        new MarketSession(LocalTime.of(9, 0), LocalTime.of(15, 30)),
                        VERIFIED_AT
                ),
                tradingDay
        );
    }

    @Test
    void 날짜별_예외가_있는_개장일에는_예외_세션과_사유를_적용한다() {
        // given
        MarketSessionSchedule schedule = new MarketSessionSchedule(List.of(
                new MarketSessionOverride(MARKET_DATE, SPECIAL_SESSION, "대학수학능력시험")
        ));

        // when
        TradingDay tradingDay = schedule.createTradingDay(
                MARKET_DATE,
                TradingDayStatus.OPEN,
                VERIFIED_AT
        );

        // then
        assertEquals(
                TradingDay.open(
                        MARKET_DATE,
                        SPECIAL_SESSION,
                        "대학수학능력시험",
                        VERIFIED_AT
                ),
                tradingDay
        );
    }

    @Test
    void 휴장일에는_날짜별_예외가_있어도_세션을_적용하지_않는다() {
        // given
        MarketSessionSchedule schedule = new MarketSessionSchedule(List.of(
                new MarketSessionOverride(MARKET_DATE, SPECIAL_SESSION, "특별 개장 예정")
        ));

        // when
        TradingDay tradingDay = schedule.createTradingDay(
                MARKET_DATE,
                TradingDayStatus.CLOSED,
                VERIFIED_AT
        );

        // then
        assertEquals(TradingDay.closed(MARKET_DATE, VERIFIED_AT), tradingDay);
    }

    @Test
    void 같은_날짜의_예외_일정이_둘_이상이면_스케줄을_만들_수_없다() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new MarketSessionSchedule(List.of(
                        new MarketSessionOverride(MARKET_DATE, SPECIAL_SESSION, "첫 번째 일정"),
                        new MarketSessionOverride(MARKET_DATE, SPECIAL_SESSION, "두 번째 일정")
                ))
        );
    }
}

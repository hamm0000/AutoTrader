package dev.hamm0000.autotrader.adapter.postgres.marketcalendar.calendar;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.hamm0000.autotrader.adapter.postgres.marketcalendar.MarketCalendarPostgresTestSupport;
import dev.hamm0000.autotrader.adapter.postgres.marketcalendar.PostgresAdapterTestApplication;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.calendar.FindTradingCalendar;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.calendar.SaveTradingCalendar;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.calendar.TradingCalendarSaveException;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.CalendarPeriod;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.MarketSession;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.TradingCalendar;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.TradingDay;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        classes = PostgresAdapterTestApplication.class,
        properties = "spring.jpa.hibernate.ddl-auto=validate"
)
class TradingCalendarPersistenceAdapterTest extends MarketCalendarPostgresTestSupport {

    private static final Instant FIRST_VERIFICATION =
            Instant.parse("2026-08-26T09:30:00Z");
    private static final Instant SECOND_VERIFICATION =
            Instant.parse("2026-09-02T09:30:00Z");
    private static final MarketSession REGULAR_SESSION = new MarketSession(
            LocalTime.of(9, 0),
            LocalTime.of(15, 30)
    );

    @Autowired
    private SaveTradingCalendar saveTradingCalendar;

    @Autowired
    private FindTradingCalendar findTradingCalendar;

    @AfterEach
    void removeFailureTrigger() {
        jdbcTemplate.execute(
                "DROP TRIGGER IF EXISTS reject_calendar_refresh_metadata "
                        + "ON trading_calendar_refresh_metadata"
        );
        jdbcTemplate.execute(
                "DROP FUNCTION IF EXISTS reject_calendar_refresh_metadata()"
        );
    }

    @Test
    void 개장일과_휴장일을_저장하면_세션과_확인_시각을_보존해_날짜순으로_조회한다() {
        // given
        CalendarPeriod period = period("2026-08-27", "2026-08-29");
        TradingCalendar calendar = new TradingCalendar(
                period,
                List.of(
                        openDay("2026-08-27", FIRST_VERIFICATION),
                        TradingDay.open(
                                date("2026-08-28"),
                                new MarketSession(LocalTime.of(10, 0), LocalTime.of(16, 30)),
                                "수능",
                                FIRST_VERIFICATION
                        ),
                        TradingDay.closed(
                                date("2026-08-29"),
                                "주말",
                                FIRST_VERIFICATION
                        )
                )
        );

        // when
        saveTradingCalendar.save(calendar);
        TradingCalendar loaded = findTradingCalendar.find(period).orElseThrow();

        // then
        assertAll(
                () -> assertEquals(period, loaded.coverage()),
                () -> assertEquals(calendar.values(), loaded.values()),
                () -> assertEquals(1, refreshMetadataCount(period, FIRST_VERIFICATION))
        );
    }

    @Test
    void 겹치는_범위를_다시_저장하면_범위_안만_갱신하고_기존_범위_밖은_보존한다() {
        // given
        saveTradingCalendar.save(new TradingCalendar(
                period("2026-08-27", "2026-08-29"),
                List.of(
                        openDay("2026-08-27", FIRST_VERIFICATION),
                        closedDay("2026-08-28", FIRST_VERIFICATION),
                        openDay("2026-08-29", FIRST_VERIFICATION)
                )
        ));
        TradingCalendar replacement = new TradingCalendar(
                period("2026-08-28", "2026-08-30"),
                List.of(
                        openDay("2026-08-28", SECOND_VERIFICATION),
                        closedDay("2026-08-29", SECOND_VERIFICATION),
                        openDay("2026-08-30", SECOND_VERIFICATION)
                )
        );

        // when
        saveTradingCalendar.save(replacement);
        TradingCalendar loaded = findTradingCalendar
                .find(period("2026-08-27", "2026-08-30"))
                .orElseThrow();

        // then
        assertEquals(
                List.of(
                        openDay("2026-08-27", FIRST_VERIFICATION),
                        openDay("2026-08-28", SECOND_VERIFICATION),
                        closedDay("2026-08-29", SECOND_VERIFICATION),
                        openDay("2026-08-30", SECOND_VERIFICATION)
                ),
                loaded.values()
        );
    }

    @Test
    void 요청_범위에_누락된_날짜가_있으면_불완전한_캘린더를_반환하지_않는다() {
        // given
        saveTradingCalendar.save(singleDayCalendar("2026-08-27"));
        saveTradingCalendar.save(singleDayCalendar("2026-08-29"));

        // when
        boolean empty = findTradingCalendar
                .find(period("2026-08-27", "2026-08-29"))
                .isEmpty();

        // then
        assertTrue(empty);
    }

    @Test
    void 갱신_메타데이터를_저장하지_못하면_기존_캘린더를_그대로_보존한다() {
        // given
        TradingCalendar original = singleDayCalendar("2026-08-27");
        saveTradingCalendar.save(original);
        jdbcTemplate.execute(
                "CREATE FUNCTION reject_calendar_refresh_metadata() RETURNS trigger "
                        + "LANGUAGE plpgsql AS 'BEGIN "
                        + "RAISE EXCEPTION ''forced refresh metadata failure''; END;'"
        );
        // 갱신 메타데이터 저장에 트리거로 강제 실패를 주입해
        // 두 테이블 저장이 한 트랜잭션으로 묶여 원자적으로 작동하는지 검증
        jdbcTemplate.execute(
                "CREATE TRIGGER reject_calendar_refresh_metadata "
                        + "BEFORE INSERT OR UPDATE "
                        + "ON trading_calendar_refresh_metadata "
                        + "FOR EACH ROW EXECUTE FUNCTION "
                        + "reject_calendar_refresh_metadata()"
        );

        // when
        assertThrows(
                TradingCalendarSaveException.class,
                () -> saveTradingCalendar.save(new TradingCalendar(
                        original.coverage(),
                        List.of(closedDay("2026-08-27", SECOND_VERIFICATION))
                ))
        );

        // then
        TradingCalendar loaded = findTradingCalendar
                .find(original.coverage())
                .orElseThrow();
        assertAll(
                () -> assertEquals(original.values(), loaded.values()),
                () -> assertEquals(
                        1,
                        refreshMetadataCount(original.coverage(), FIRST_VERIFICATION)
                )
        );
    }

    private int refreshMetadataCount(CalendarPeriod period, Instant refreshedAt) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM trading_calendar_refresh_metadata "
                        + "WHERE coverage_from = ? "
                        + "AND coverage_through = ? "
                        + "AND last_successful_refresh_at = "
                        + "CAST(? AS TIMESTAMP WITH TIME ZONE)",
                Integer.class,
                Date.valueOf(period.from()),
                Date.valueOf(period.through()),
                refreshedAt.toString()
        );
    }

    private static TradingCalendar singleDayCalendar(String value) {
        return new TradingCalendar(
                period(value, value),
                List.of(openDay(value, FIRST_VERIFICATION))
        );
    }

    private static TradingDay openDay(String value, Instant verifiedAt) {
        return TradingDay.open(date(value), REGULAR_SESSION, verifiedAt);
    }

    private static TradingDay closedDay(String value, Instant verifiedAt) {
        return TradingDay.closed(date(value), verifiedAt);
    }

    private static CalendarPeriod period(String from, String through) {
        return new CalendarPeriod(date(from), date(through));
    }

    private static LocalDate date(String value) {
        return LocalDate.parse(value);
    }
}

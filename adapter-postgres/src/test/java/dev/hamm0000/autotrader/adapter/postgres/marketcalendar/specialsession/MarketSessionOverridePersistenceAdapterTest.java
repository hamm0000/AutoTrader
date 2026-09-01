package dev.hamm0000.autotrader.adapter.postgres.marketcalendar.specialsession;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.hamm0000.autotrader.adapter.postgres.marketcalendar.MarketCalendarPostgresTestSupport;
import dev.hamm0000.autotrader.adapter.postgres.marketcalendar.PostgresAdapterTestApplication;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.specialsession.FindMarketSessionOverrides;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.CalendarPeriod;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.MarketSession;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.MarketSessionOverride;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        classes = PostgresAdapterTestApplication.class,
        properties = "spring.jpa.hibernate.ddl-auto=validate"
)
class MarketSessionOverridePersistenceAdapterTest
        extends MarketCalendarPostgresTestSupport {

    @Autowired
    private FindMarketSessionOverrides findMarketSessionOverrides;

    @Test
    void 날짜별_예외_세션을_조회하면_요청_범위의_일정만_날짜순으로_반환한다() {
        // given
        insertOverride("2026-01-02", "10:00", "15:30", "연초 개장일");
        insertOverride("2026-11-12", "10:00", "16:30", "수능");
        insertOverride("2026-12-31", "09:00", "14:30", "연말 단축 운영");

        // when
        List<MarketSessionOverride> overrides = findMarketSessionOverrides.find(
                period("2026-11-01", "2026-12-31")
        );

        // then
        assertEquals(
                List.of(
                        override("2026-11-12", "10:00", "16:30", "수능"),
                        override("2026-12-31", "09:00", "14:30", "연말 단축 운영")
                ),
                overrides
        );
    }

    //아직 저장 포트가 없어 테스트 데이터를 직접 넣어 조회 매핑만 검증한다.
    private void insertOverride(
            String date,
            String opensAt,
            String closesAt,
            String note
    ) {
        jdbcTemplate.update(
                "INSERT INTO market_session_override "
                        + "(trade_date, session_open, session_close, note) "
                        + "VALUES (?, ?, ?, ?)",
                Date.valueOf(date(date)),
                Time.valueOf(time(opensAt)),
                Time.valueOf(time(closesAt)),
                note
        );
    }

    private static MarketSessionOverride override(
            String date,
            String opensAt,
            String closesAt,
            String note
    ) {
        return new MarketSessionOverride(
                date(date),
                new MarketSession(time(opensAt), time(closesAt)),
                note
        );
    }

    private static CalendarPeriod period(String from, String through) {
        return new CalendarPeriod(date(from), date(through));
    }

    private static LocalDate date(String value) {
        return LocalDate.parse(value);
    }

    private static LocalTime time(String value) {
        return LocalTime.parse(value);
    }
}

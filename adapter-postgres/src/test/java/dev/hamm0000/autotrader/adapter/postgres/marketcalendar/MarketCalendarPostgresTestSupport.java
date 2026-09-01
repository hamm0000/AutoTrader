package dev.hamm0000.autotrader.adapter.postgres.marketcalendar;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public abstract class MarketCalendarPostgresTestSupport {

    @Autowired
    protected JdbcTemplate jdbcTemplate;


    // calendar/specialsession 테스트가 Postgres 컨테이너 하나를 공유하므로
    // 한 테스트가 남긴 데이터가 다른 테스트에 영향을 미치지 않도록 테이블을 매번 초기화 함
    @BeforeEach
    protected void clearTables() {
        jdbcTemplate.execute(
                "TRUNCATE TABLE market_session_override, "
                        + "trading_calendar_refresh_metadata, trading_calendar"
        );
    }
}

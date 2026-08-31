package dev.hamm0000.autotrader.adapter.postgres.marketcalendar.specialsession;

import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.specialsession.FindMarketSessionOverrides;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.specialsession.MarketSessionOverrideQueryException;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.CalendarPeriod;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.MarketSessionOverride;
import java.util.List;
import java.util.Objects;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 정규장과 운영 시간이 다른 날짜의 세션 정보를 조회합니다.
 */
@Repository
public class MarketSessionOverridePersistenceAdapter implements FindMarketSessionOverrides {

    private final MarketSessionOverrideRepository overrides;

    MarketSessionOverridePersistenceAdapter(MarketSessionOverrideRepository overrides) {
        this.overrides = Objects.requireNonNull(overrides, "overrides must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarketSessionOverride> find(CalendarPeriod period) {
        Objects.requireNonNull(period, "period must not be null");

        List<MarketSessionOverrideEntity> entities;
        try {
            entities = overrides.findByTradeDateBetweenOrderByTradeDateAsc(
                    period.from(),
                    period.through()
            );
        } catch (DataAccessException exception) {
            throw new MarketSessionOverrideQueryException(
                    "날짜별 예외 세션을 조회하지 못했습니다",
                    exception
            );
        }

        try {
            return entities.stream()
                    .map(MarketSessionOverrideEntityMapper::toDomain)
                    .toList();
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new MarketSessionOverrideQueryException(
                    "저장된 날짜별 예외 세션이 유효하지 않습니다",
                    exception
            );
        }
    }
}

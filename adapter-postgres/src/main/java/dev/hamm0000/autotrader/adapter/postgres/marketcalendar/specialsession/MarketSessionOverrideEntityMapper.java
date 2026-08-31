package dev.hamm0000.autotrader.adapter.postgres.marketcalendar.specialsession;

import dev.hamm0000.autotrader.trading.marketcalendar.domain.MarketSession;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.MarketSessionOverride;

final class MarketSessionOverrideEntityMapper {

    private MarketSessionOverrideEntityMapper() {
    }

    static MarketSessionOverride toDomain(MarketSessionOverrideEntity entity) {
        return new MarketSessionOverride(
                entity.tradeDate(),
                new MarketSession(entity.sessionOpen(), entity.sessionClose()),
                entity.note()
        );
    }
}

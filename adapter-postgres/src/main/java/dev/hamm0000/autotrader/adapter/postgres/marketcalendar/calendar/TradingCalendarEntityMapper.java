package dev.hamm0000.autotrader.adapter.postgres.marketcalendar.calendar;

import dev.hamm0000.autotrader.trading.marketcalendar.domain.MarketSession;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.TradingDay;

final class TradingCalendarEntityMapper {

    private TradingCalendarEntityMapper() {
    }

    static TradingDayEntity toEntity(TradingDay tradingDay) {
        MarketSession session = tradingDay.session().orElse(null);
        return new TradingDayEntity(
                tradingDay.date(),
                tradingDay.isTradingDay(),
                session == null ? null : session.opensAt(),
                session == null ? null : session.closesAt(),
                tradingDay.note().orElse(null),
                tradingDay.verifiedAt()
        );
    }

    static TradingDay toDomain(TradingDayEntity entity) {
        if (!entity.tradingDay()) {
            if (entity.note() == null) {
                return TradingDay.closed(entity.tradeDate(), entity.verifiedAt());
            }
            return TradingDay.closed(entity.tradeDate(), entity.note(), entity.verifiedAt());
        }

        MarketSession session = new MarketSession(entity.sessionOpen(), entity.sessionClose());
        if (entity.note() == null) {
            return TradingDay.open(entity.tradeDate(), session, entity.verifiedAt());
        }
        return TradingDay.open(
                entity.tradeDate(),
                session,
                entity.note(),
                entity.verifiedAt()
        );
    }
}

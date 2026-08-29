package dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.specialsession;

import dev.hamm0000.autotrader.trading.marketcalendar.domain.CalendarPeriod;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.MarketSessionOverride;
import java.util.List;

@FunctionalInterface
public interface FindMarketSessionOverrides {

    /**
     * 기간 내에 DB로부터 예외로 등록된 날이 있는지 조회합니다.
     */
    List<MarketSessionOverride> find(CalendarPeriod period);
}

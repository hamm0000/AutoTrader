package dev.hamm0000.autotrader.trading.risk.dividend.application.port.out;

import dev.hamm0000.autotrader.trading.risk.dividend.domain.DividendEvent;
import java.util.List;

@FunctionalInterface
public interface LoadDividendEvents {

    /**
     * 조회 구간에 배당락일이 포함되는 배당 이벤트를 조회합니다.
     */
    List<DividendEvent> load(DividendEventsQuery query);
}

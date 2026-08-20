package dev.hamm0000.autotrader.trading.marketdata.application.port.out;

import dev.hamm0000.autotrader.trading.marketdata.domain.RecentClosingPrices;

@FunctionalInterface
public interface LoadRecentClosingPrices {

    /**
     * 지정한 기준일 이전에 완료된 거래일의 수정 종가를 조회한다.
     *
     */
    RecentClosingPrices load(RecentClosingPricesQuery query);
}

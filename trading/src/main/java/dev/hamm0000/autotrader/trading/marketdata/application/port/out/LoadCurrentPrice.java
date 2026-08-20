package dev.hamm0000.autotrader.trading.marketdata.application.port.out;

import dev.hamm0000.autotrader.trading.marketdata.domain.CurrentPrice;

@FunctionalInterface
public interface LoadCurrentPrice {

    CurrentPrice load(CurrentPriceQuery query);
}

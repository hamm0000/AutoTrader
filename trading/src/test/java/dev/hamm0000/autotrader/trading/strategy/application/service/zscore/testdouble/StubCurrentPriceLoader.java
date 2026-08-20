package dev.hamm0000.autotrader.trading.strategy.application.service.zscore.testdouble;

import dev.hamm0000.autotrader.trading.marketdata.application.port.out.CurrentPriceQuery;
import dev.hamm0000.autotrader.trading.marketdata.application.port.out.LoadCurrentPrice;
import dev.hamm0000.autotrader.trading.marketdata.domain.CurrentPrice;

public final class StubCurrentPriceLoader implements LoadCurrentPrice {

    private final CurrentPrice result;
    private RuntimeException failure;
    private CurrentPriceQuery query;
    private int loadCount;

    public StubCurrentPriceLoader(CurrentPrice result) {
        this.result = result;
    }

    @Override
    public CurrentPrice load(CurrentPriceQuery query) {
        loadCount++;
        this.query = query;
        if (failure != null) {
            throw failure;
        }
        return result;
    }

    public void failWith(RuntimeException failure) {
        this.failure = failure;
    }

    public CurrentPriceQuery query() {
        return query;
    }

    public int loadCount() {
        return loadCount;
    }
}

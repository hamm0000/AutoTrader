package dev.hamm0000.autotrader.trading.strategy.zscore.application.service.testdouble;

import dev.hamm0000.autotrader.trading.marketdata.application.port.out.LoadRecentClosingPrices;
import dev.hamm0000.autotrader.trading.marketdata.application.port.out.RecentClosingPricesQuery;
import dev.hamm0000.autotrader.trading.marketdata.domain.RecentClosingPrices;

public final class StubRecentClosingPricesLoader implements LoadRecentClosingPrices {

    private final RecentClosingPrices result;
    private RuntimeException failure;
    private RecentClosingPricesQuery query;
    private int loadCount;

    public StubRecentClosingPricesLoader(RecentClosingPrices result) {
        this.result = result;
    }

    @Override
    public RecentClosingPrices load(RecentClosingPricesQuery query) {
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

    public RecentClosingPricesQuery query() {
        return query;
    }

    public int loadCount() {
        return loadCount;
    }
}

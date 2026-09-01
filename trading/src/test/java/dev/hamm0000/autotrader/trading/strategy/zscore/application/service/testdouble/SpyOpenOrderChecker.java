package dev.hamm0000.autotrader.trading.strategy.zscore.application.service.testdouble;

import dev.hamm0000.autotrader.trading.order.application.port.out.CheckOpenOrder;
import dev.hamm0000.autotrader.trading.order.application.port.out.OpenOrderQuery;

public final class SpyOpenOrderChecker implements CheckOpenOrder {

    private final boolean result;
    private OpenOrderQuery query;
    private int checkCount;

    public SpyOpenOrderChecker(boolean result) {
        this.result = result;
    }

    @Override
    public boolean exists(OpenOrderQuery query) {
        checkCount++;
        this.query = query;
        return result;
    }

    public OpenOrderQuery query() {
        return query;
    }

    public int checkCount() {
        return checkCount;
    }
}

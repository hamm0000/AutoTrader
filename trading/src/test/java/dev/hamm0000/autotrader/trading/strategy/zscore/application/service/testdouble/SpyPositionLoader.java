package dev.hamm0000.autotrader.trading.strategy.zscore.application.service.testdouble;

import dev.hamm0000.autotrader.trading.position.application.port.out.LoadPosition;
import dev.hamm0000.autotrader.trading.position.application.port.out.PositionQuery;
import dev.hamm0000.autotrader.trading.position.domain.Position;
import java.util.Objects;

public final class SpyPositionLoader implements LoadPosition {

    private final Position result;
    private PositionQuery query;
    private int loadCount;

    public SpyPositionLoader(Position result) {
        this.result = Objects.requireNonNull(result, "result must not be null");
    }

    @Override
    public Position load(PositionQuery query) {
        loadCount++;
        this.query = query;
        return result;
    }

    public PositionQuery query() {
        return query;
    }

    public int loadCount() {
        return loadCount;
    }
}

package dev.hamm0000.autotrader.trading.risk.dividend.application.service.testdouble;

import dev.hamm0000.autotrader.trading.risk.dividend.application.port.out.DividendEventQueryException;
import dev.hamm0000.autotrader.trading.risk.dividend.application.port.out.DividendEventsQuery;
import dev.hamm0000.autotrader.trading.risk.dividend.application.port.out.LoadDividendEvents;
import dev.hamm0000.autotrader.trading.risk.dividend.domain.DividendEvent;
import java.util.List;

public final class StubDividendEventsLoader implements LoadDividendEvents {

    private final List<DividendEvent> events;
    private DividendEventsQuery query;
    private DividendEventQueryException failure;

    public StubDividendEventsLoader(List<DividendEvent> events) {
        this.events = List.copyOf(events);
    }

    @Override
    public List<DividendEvent> load(DividendEventsQuery query) {
        this.query = query;
        if (failure != null) {
            throw failure;
        }
        return events;
    }

    public void failWith(DividendEventQueryException failure) {
        this.failure = failure;
    }

    public DividendEventsQuery query() {
        return query;
    }
}

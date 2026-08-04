package dev.hamm0000.autotrader.adapter.kis.auth.testdouble;

import java.time.Instant;
import java.time.InstantSource;

public final class FakeInstantSource implements InstantSource {

    private volatile Instant instant;

    public FakeInstantSource(Instant instant) {
        this.instant = instant;
    }

    public void moveTo(Instant target) {
        this.instant = target;
    }

    @Override
    public Instant instant() {
        return instant;
    }
}

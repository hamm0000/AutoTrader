package dev.hamm0000.autotrader.trading.position.application.port.out;

import dev.hamm0000.autotrader.trading.position.domain.Position;

@FunctionalInterface
public interface LoadPosition {

    Position load(PositionQuery query);
}

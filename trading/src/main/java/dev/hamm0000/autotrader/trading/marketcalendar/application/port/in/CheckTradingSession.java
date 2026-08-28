package dev.hamm0000.autotrader.trading.marketcalendar.application.port.in;

import dev.hamm0000.autotrader.trading.marketcalendar.domain.TradingSessionDecision;

@FunctionalInterface
public interface CheckTradingSession {

    TradingSessionDecision check(TradingSessionCheckCommand command);
}

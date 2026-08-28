package dev.hamm0000.autotrader.trading.marketcalendar.application.service;

import dev.hamm0000.autotrader.trading.marketcalendar.application.port.in.CheckTradingSession;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.in.TradingSessionCheckCommand;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.FindTradingCalendar;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.TradingCalendarQueryException;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.CalendarPeriod;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.TradingCalendar;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.TradingSessionDecision;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.TradingSessionPolicy;
import java.time.InstantSource;
import java.util.Objects;
import java.util.Optional;

/**
 * 저장된 거래 캘린더를 조회해 런타임의 시장 준비 여부를 판단합니다.
 * 이때 캘린더 부재는 부팅 실패로 전파하지 않고 degraed로 시작합니다.
 */
public final class TradingSessionService implements CheckTradingSession {

    private final FindTradingCalendar findTradingCalendar;
    private final TradingSessionPolicy policy;
    private final InstantSource timeSource;

    public TradingSessionService(
            FindTradingCalendar findTradingCalendar,
            TradingSessionPolicy policy,
            InstantSource timeSource
    ) {
        this.findTradingCalendar = Objects.requireNonNull(
                findTradingCalendar,
                "findTradingCalendar must not be null"
        );
        this.policy = Objects.requireNonNull(policy, "policy must not be null");
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource must not be null");
    }

    @Override
    public TradingSessionDecision check(TradingSessionCheckCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        CalendarPeriod period = new CalendarPeriod(
                command.marketDate(),
                command.marketDate()
        );

        try {
            Optional<TradingCalendar> calendar = Objects.requireNonNull(
                    findTradingCalendar.find(period),
                    "found calendar must not be null"
            );
            if (calendar.isEmpty()) {
                return TradingSessionDecision.startDegraded(
                        TradingSessionDecision.Reason.CALENDAR_MISSING
                );
            }
            return policy.decide(
                    calendar.get(),
                    command.marketDate(),
                    timeSource.instant()
            );
        } catch (TradingCalendarQueryException failure) {
            return TradingSessionDecision.startDegraded(
                    TradingSessionDecision.Reason.CALENDAR_UNAVAILABLE
            );
        }
    }
}

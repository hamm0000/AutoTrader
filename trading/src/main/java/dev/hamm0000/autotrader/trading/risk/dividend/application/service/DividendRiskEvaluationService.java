package dev.hamm0000.autotrader.trading.risk.dividend.application.service;

import dev.hamm0000.autotrader.trading.marketdata.domain.RecentClosingPrices;
import dev.hamm0000.autotrader.trading.risk.dividend.application.port.in.DividendRiskEvaluationCommand;
import dev.hamm0000.autotrader.trading.risk.dividend.application.port.in.EvaluateDividendRisk;
import dev.hamm0000.autotrader.trading.risk.dividend.application.port.out.DividendEventQueryException;
import dev.hamm0000.autotrader.trading.risk.dividend.application.port.out.DividendEventsQuery;
import dev.hamm0000.autotrader.trading.risk.dividend.application.port.out.LoadDividendEvents;
import dev.hamm0000.autotrader.trading.risk.dividend.domain.DividendEvent;
import dev.hamm0000.autotrader.trading.risk.dividend.domain.DividendRiskAssessment;
import dev.hamm0000.autotrader.trading.risk.dividend.domain.DividendRiskPolicy;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public final class DividendRiskEvaluationService implements EvaluateDividendRisk {

    private final LoadDividendEvents loadDividendEvents;
    private final DividendRiskPolicy policy;

    public DividendRiskEvaluationService(
            LoadDividendEvents loadDividendEvents,
            DividendRiskPolicy policy
    ) {
        this.loadDividendEvents = Objects.requireNonNull(
                loadDividendEvents,
                "loadDividendEvents must not be null"
        );
        this.policy = Objects.requireNonNull(policy, "policy must not be null");
    }

    @Override
    public DividendRiskAssessment evaluate(DividendRiskEvaluationCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        RecentClosingPrices closingPrices = command.closingPrices();
        LocalDate evaluationDate = command.evaluationDate();
        LocalDate oldestClosingDate = closingPrices.values().get(0).tradingDate();
        DividendEventsQuery query = new DividendEventsQuery(
                command.symbol(),
                oldestClosingDate,
                evaluationDate
        );

        try {
            List<DividendEvent> events = loadDividendEvents.load(query);
            return policy.assess(oldestClosingDate, evaluationDate, events);
        } catch (DividendEventQueryException exception) {
            if (exception.reason() == DividendEventQueryException.Reason.UNSUPPORTED_ENVIRONMENT) {
                return DividendRiskAssessment.SKIP;
            }
            throw exception;
        }
    }
}

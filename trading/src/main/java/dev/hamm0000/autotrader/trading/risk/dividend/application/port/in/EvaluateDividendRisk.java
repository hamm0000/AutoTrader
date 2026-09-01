package dev.hamm0000.autotrader.trading.risk.dividend.application.port.in;

import dev.hamm0000.autotrader.trading.risk.dividend.domain.DividendRiskAssessment;

@FunctionalInterface
public interface EvaluateDividendRisk {

    DividendRiskAssessment evaluate(DividendRiskEvaluationCommand command);
}

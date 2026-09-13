package dev.hamm0000.autotrader.trading.strategy.zscore.application.service;

import dev.hamm0000.autotrader.trading.position.application.port.out.LoadPosition;
import dev.hamm0000.autotrader.trading.position.application.port.out.PositionQuery;
import dev.hamm0000.autotrader.trading.position.domain.Position;
import dev.hamm0000.autotrader.trading.proposal.domain.TradeProposal;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.EvaluateZScore;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.EvaluatedZScore;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.PlanZScoreTrade;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreTradePlan;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreTradePlanningCommand;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.FindZScoreTradePlan;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.SaveZScoreTradePlan;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.SaveZScoreTradePlanCommand;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.ZScoreTradePlanQueryException;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.ZScoreTradePlanSaveException;
import dev.hamm0000.autotrader.trading.strategy.zscore.domain.StrategySignal;
import dev.hamm0000.autotrader.trading.strategy.zscore.domain.ZScoreTradeProposalPolicy;
import java.time.InstantSource;
import java.util.Objects;
import java.util.Optional;

public final class ZScoreTradePlanningService implements PlanZScoreTrade {

    private final EvaluateZScore evaluateZScore;
    private final LoadPosition loadPosition;
    private final ZScoreTradeProposalPolicy proposalPolicy;
    private final FindZScoreTradePlan findZScoreTradePlan;
    private final SaveZScoreTradePlan saveZScoreTradePlan;
    private final InstantSource timeSource;

    public ZScoreTradePlanningService(
            EvaluateZScore evaluateZScore,
            LoadPosition loadPosition,
            ZScoreTradeProposalPolicy proposalPolicy,
            FindZScoreTradePlan findZScoreTradePlan,
            SaveZScoreTradePlan saveZScoreTradePlan,
            InstantSource timeSource
    ) {
        this.evaluateZScore = Objects.requireNonNull(evaluateZScore, "evaluateZScore must not be null");
        this.loadPosition = Objects.requireNonNull(loadPosition, "loadPosition must not be null");
        this.proposalPolicy = Objects.requireNonNull(
                proposalPolicy,
                "proposalPolicy must not be null"
        );
        this.findZScoreTradePlan = Objects.requireNonNull(
                findZScoreTradePlan,
                "findZScoreTradePlan must not be null"
        );
        this.saveZScoreTradePlan = Objects.requireNonNull(
                saveZScoreTradePlan,
                "saveZScoreTradePlan must not be null"
        );
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource must not be null");
    }

    @Override
    public ZScoreTradePlan plan(ZScoreTradePlanningCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        Optional<ZScoreTradePlan> existing = findZScoreTradePlan.findByPlanningCommand(command);
        if (existing.isPresent()) {
            return existing.get();
        }

        EvaluatedZScore evaluation = evaluateZScore.evaluate(command.evaluationCommand());
        StrategySignal signal = evaluation.analytics().signal();
        ZScoreTradePlan plan;
        if (signal == StrategySignal.HOLD) {
            plan = new ZScoreTradePlan(evaluation, Optional.empty());
        } else {
            plan = planProposal(command, evaluation, signal);
        }
        try {
            saveZScoreTradePlan.save(new SaveZScoreTradePlanCommand(
                    command,
                    plan,
                    timeSource.instant()
            ));
        } catch (ZScoreTradePlanSaveException saveFailure) {
            return recoverPlan(command, saveFailure);
        }
        return plan;
    }

    private ZScoreTradePlan recoverPlan(
            ZScoreTradePlanningCommand command,
            ZScoreTradePlanSaveException saveFailure
    ) {
        try {
            return findZScoreTradePlan.findByPlanningCommand(command)
                    .orElseThrow(() -> saveFailure);
        } catch (ZScoreTradePlanQueryException queryFailure) {
            saveFailure.addSuppressed(queryFailure);
            throw saveFailure;
        }
    }

    private ZScoreTradePlan planProposal(
            ZScoreTradePlanningCommand command,
            EvaluatedZScore evaluation,
            StrategySignal signal
    ) {
        Position position = loadPosition.load(new PositionQuery(evaluation.symbol()));
        Optional<TradeProposal> proposal = proposalPolicy.propose(
                evaluation.symbol(),
                evaluation.currentPrice(),
                command.purchaseBudget(),
                signal,
                position
        );
        return new ZScoreTradePlan(evaluation, proposal);
    }
}

package dev.hamm0000.autotrader.trading.strategy.zscore.application.service;

import dev.hamm0000.autotrader.trading.order.application.port.out.CheckOpenOrder;
import dev.hamm0000.autotrader.trading.order.application.port.out.OpenOrderQuery;
import dev.hamm0000.autotrader.trading.order.domain.OrderSide;
import dev.hamm0000.autotrader.trading.position.application.port.out.LoadPosition;
import dev.hamm0000.autotrader.trading.position.application.port.out.PositionQuery;
import dev.hamm0000.autotrader.trading.position.domain.Position;
import dev.hamm0000.autotrader.trading.proposal.domain.TradeProposal;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.EvaluateZScore;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.EvaluatedZScore;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.PlanZScoreTrade;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreTradePlan;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreTradePlanningCommand;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.SaveZScoreTradePlan;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.SaveZScoreTradePlanCommand;
import dev.hamm0000.autotrader.trading.strategy.zscore.domain.StrategySignal;
import dev.hamm0000.autotrader.trading.strategy.zscore.domain.ZScoreTradeProposalPolicy;
import java.time.InstantSource;
import java.util.Objects;
import java.util.Optional;

public final class ZScoreTradePlanningService implements PlanZScoreTrade {

    private final EvaluateZScore evaluateZScore;
    private final LoadPosition loadPosition;
    private final CheckOpenOrder checkOpenOrder;
    private final ZScoreTradeProposalPolicy proposalPolicy;
    private final SaveZScoreTradePlan saveZScoreTradePlan;
    private final InstantSource timeSource;

    public ZScoreTradePlanningService(
            EvaluateZScore evaluateZScore,
            LoadPosition loadPosition,
            CheckOpenOrder checkOpenOrder,
            ZScoreTradeProposalPolicy proposalPolicy,
            SaveZScoreTradePlan saveZScoreTradePlan,
            InstantSource timeSource
    ) {
        this.evaluateZScore = Objects.requireNonNull(evaluateZScore, "evaluateZScore must not be null");
        this.loadPosition = Objects.requireNonNull(loadPosition, "loadPosition must not be null");
        this.checkOpenOrder = Objects.requireNonNull(checkOpenOrder, "checkOpenOrder must not be null");
        this.proposalPolicy = Objects.requireNonNull(
                proposalPolicy,
                "proposalPolicy must not be null"
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

        EvaluatedZScore evaluation = evaluateZScore.evaluate(command.evaluationCommand());
        StrategySignal signal = evaluation.analytics().signal();
        ZScoreTradePlan plan;
        if (signal == StrategySignal.HOLD) {
            plan = new ZScoreTradePlan(evaluation, Optional.empty());
        } else {
            plan = planOrder(command, evaluation, signal);
        }
        saveZScoreTradePlan.save(new SaveZScoreTradePlanCommand(
                command,
                plan,
                timeSource.instant()
        ));
        return plan;
    }

    private ZScoreTradePlan planOrder(
            ZScoreTradePlanningCommand command,
            EvaluatedZScore evaluation,
            StrategySignal signal
    ) {
        OrderSide side = signal == StrategySignal.BUY ? OrderSide.BUY : OrderSide.SELL;
        Position position = loadPosition.load(new PositionQuery(evaluation.symbol()));
        boolean hasOpenOrder = checkOpenOrder.exists(new OpenOrderQuery(evaluation.symbol(), side));
        Optional<TradeProposal> proposal = proposalPolicy.propose(
                evaluation.symbol(),
                evaluation.currentPrice(),
                command.purchaseBudget(),
                signal,
                position,
                hasOpenOrder
        );
        return new ZScoreTradePlan(evaluation, proposal);
    }
}

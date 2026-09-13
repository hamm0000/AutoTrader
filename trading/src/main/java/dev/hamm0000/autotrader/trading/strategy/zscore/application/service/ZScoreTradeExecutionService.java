package dev.hamm0000.autotrader.trading.strategy.zscore.application.service;

import dev.hamm0000.autotrader.trading.order.application.port.in.OrderPlacementCommand;
import dev.hamm0000.autotrader.trading.order.application.port.in.PlaceOrder;
import dev.hamm0000.autotrader.trading.order.domain.Order;
import dev.hamm0000.autotrader.trading.order.domain.OrderIdempotencyKey;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ExecuteZScoreTrade;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.PlanZScoreTrade;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreEvaluationCommand;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreTradeExecution;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreTradePlan;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreTradePlanningCommand;
import java.util.Objects;
import java.util.Optional;

public final class ZScoreTradeExecutionService implements ExecuteZScoreTrade {

    private static final String STRATEGY_NAME = "zscore";

    private final PlanZScoreTrade planZScoreTrade;
    private final PlaceOrder placeOrder;

    public ZScoreTradeExecutionService(
            PlanZScoreTrade planZScoreTrade,
            PlaceOrder placeOrder
    ) {
        this.planZScoreTrade = Objects.requireNonNull(
                planZScoreTrade,
                "planZScoreTrade must not be null"
        );
        this.placeOrder = Objects.requireNonNull(placeOrder, "placeOrder must not be null");
    }

    @Override
    public ZScoreTradeExecution execute(ZScoreTradePlanningCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        ZScoreTradePlan plan = planZScoreTrade.plan(command);
        Optional<Order> order = plan.proposal()
                .map(proposal -> placeOrder.place(new OrderPlacementCommand(
                        idempotencyKey(command.evaluationCommand()),
                        proposal
                )));
        return new ZScoreTradeExecution(plan, order);
    }

    private static OrderIdempotencyKey idempotencyKey(
            ZScoreEvaluationCommand evaluationCommand
    ) {
        return new OrderIdempotencyKey(
                STRATEGY_NAME
                        + ":"
                        + evaluationCommand.symbol()
                        + ":"
                        + evaluationCommand.endDateExclusive()
        );
    }
}

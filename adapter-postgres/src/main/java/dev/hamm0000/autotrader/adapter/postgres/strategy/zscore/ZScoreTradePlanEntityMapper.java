package dev.hamm0000.autotrader.adapter.postgres.strategy.zscore;

import dev.hamm0000.autotrader.trading.order.domain.OrderSide;
import dev.hamm0000.autotrader.trading.proposal.domain.BuyProposal;
import dev.hamm0000.autotrader.trading.proposal.domain.SellProposal;
import dev.hamm0000.autotrader.trading.proposal.domain.TradeProposal;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.EvaluatedZScore;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreEvaluationCommand;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.SaveZScoreTradePlanCommand;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.ZScoreEvaluationResult;
import java.math.BigDecimal;
import java.util.UUID;

final class ZScoreTradePlanEntityMapper {

    private ZScoreTradePlanEntityMapper() {
    }

    static ZScoreTradePlanEntity toEntity(SaveZScoreTradePlanCommand command) {
        ZScoreEvaluationCommand evaluationCommand = command
                .planningCommand()
                .evaluationCommand();
        EvaluatedZScore evaluation = command.plan().evaluation();
        ZScoreEvaluationResult analytics = evaluation.analytics();
        ProposalColumns proposal = proposalColumns(command.plan().proposal().orElse(null));

        return new ZScoreTradePlanEntity(
                UUID.randomUUID(),
                evaluation.symbol(),
                evaluationCommand.endDateExclusive(),
                evaluationCommand.window(),
                command.plannedAt(),
                evaluation.currentPrice().value(),
                analytics.zScore(),
                analytics.rollingMean(),
                analytics.rollingStddev(),
                analytics.signal(),
                analytics.reason(),
                proposal.side(),
                proposal.quantity(),
                proposal.price(),
                command.planningCommand().purchaseBudget()
        );
    }

    private static ProposalColumns proposalColumns(TradeProposal proposal) {
        if (proposal == null) {
            return new ProposalColumns(null, null, null);
        }
        if (proposal instanceof BuyProposal buy) {
            return new ProposalColumns(
                    OrderSide.BUY,
                    buy.quantity(),
                    buy.currentPrice()
            );
        }
        SellProposal sell = (SellProposal) proposal;
        return new ProposalColumns(OrderSide.SELL, sell.quantity(), null);
    }

    private record ProposalColumns(
            OrderSide side,
            Long quantity,
            BigDecimal price
    ) {
    }
}

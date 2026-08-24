package dev.hamm0000.autotrader.trading.strategy.zscore.application.service;

import dev.hamm0000.autotrader.trading.marketdata.application.port.out.CurrentPriceQuery;
import dev.hamm0000.autotrader.trading.marketdata.application.port.out.LoadCurrentPrice;
import dev.hamm0000.autotrader.trading.marketdata.application.port.out.LoadRecentClosingPrices;
import dev.hamm0000.autotrader.trading.marketdata.application.port.out.RecentClosingPricesQuery;
import dev.hamm0000.autotrader.trading.marketdata.domain.CurrentPrice;
import dev.hamm0000.autotrader.trading.marketdata.domain.DailyClosingPrice;
import dev.hamm0000.autotrader.trading.marketdata.domain.RecentClosingPrices;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.EvaluateZScore;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.in.ZScoreEvaluationCommand;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.EvaluateZScoreStrategy;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.ZScoreEvaluationRequest;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.ZScoreEvaluationResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public final class ZScoreEvaluationService implements EvaluateZScore {

    private final LoadRecentClosingPrices loadRecentClosingPrices;
    private final LoadCurrentPrice loadCurrentPrice;
    private final EvaluateZScoreStrategy evaluateZScoreStrategy;

    public ZScoreEvaluationService(
            LoadRecentClosingPrices loadRecentClosingPrices,
            LoadCurrentPrice loadCurrentPrice,
            EvaluateZScoreStrategy evaluateZScoreStrategy
    ) {
        this.loadRecentClosingPrices = Objects.requireNonNull(
                loadRecentClosingPrices,
                "loadRecentClosingPrices must not be null"
        );
        this.loadCurrentPrice = Objects.requireNonNull(
                loadCurrentPrice,
                "loadCurrentPrice must not be null"
        );
        this.evaluateZScoreStrategy = Objects.requireNonNull(
                evaluateZScoreStrategy,
                "evaluateZScoreStrategy must not be null"
        );
    }

    @Override
    public ZScoreEvaluationResult evaluate(ZScoreEvaluationCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        RecentClosingPrices closingPrices = loadRecentClosingPrices.load(
                new RecentClosingPricesQuery(
                        command.symbol(),
                        command.window(),
                        command.endDateExclusive()
                )
        );
        CurrentPrice currentPrice = loadCurrentPrice.load(new CurrentPriceQuery(command.symbol()));

        return evaluateZScoreStrategy.evaluate(new ZScoreEvaluationRequest(
                command.symbol(),
                command.window(),
                toDoublePrices(closingPrices.values()),
                currentPrice.value().doubleValue()
        ));
    }

    private static List<Double> toDoublePrices(List<DailyClosingPrice> closingPrices) {
        return closingPrices.stream()
                .map(closingPrice -> closingPrice.price().doubleValue())
                .toList();
    }
}

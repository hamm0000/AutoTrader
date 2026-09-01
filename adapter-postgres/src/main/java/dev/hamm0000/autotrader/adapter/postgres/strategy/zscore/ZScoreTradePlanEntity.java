package dev.hamm0000.autotrader.adapter.postgres.strategy.zscore;

import dev.hamm0000.autotrader.trading.order.domain.OrderSide;
import dev.hamm0000.autotrader.trading.strategy.zscore.domain.StrategySignal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "zscore_trade_plan",
        uniqueConstraints = @UniqueConstraint(
                name = "zscore_trade_plan_symbol_date_key",
                columnNames = {"symbol", "end_date_exclusive"}
        )
)
public class ZScoreTradePlanEntity {

    @Id
    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "symbol", nullable = false, length = 12)
    private String symbol;

    @Column(name = "end_date_exclusive", nullable = false)
    private LocalDate endDateExclusive;

    @Column(name = "lookback_window", nullable = false)
    private int window;

    @Column(name = "planned_at", nullable = false)
    private Instant plannedAt;

    @Column(name = "current_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentPrice;

    @Column(name = "z_score", nullable = false)
    private double zScore;

    @Column(name = "rolling_mean", nullable = false)
    private double rollingMean;

    @Column(name = "rolling_stddev", nullable = false)
    private double rollingStddev;

    @Enumerated(EnumType.STRING)
    @Column(name = "signal", nullable = false, length = 8)
    private StrategySignal signal;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "proposal_side", length = 8)
    private OrderSide proposalSide;

    @Column(name = "proposed_quantity")
    private Long proposedQuantity;

    @Column(name = "proposed_price", precision = 19, scale = 4)
    private BigDecimal proposedPrice;

    @Column(name = "purchase_budget", nullable = false, precision = 19, scale = 4)
    private BigDecimal purchaseBudget;

    protected ZScoreTradePlanEntity() {
    }

    ZScoreTradePlanEntity(
            UUID planId,
            String symbol,
            LocalDate endDateExclusive,
            int window,
            Instant plannedAt,
            BigDecimal currentPrice,
            double zScore,
            double rollingMean,
            double rollingStddev,
            StrategySignal signal,
            String reason,
            OrderSide proposalSide,
            Long proposedQuantity,
            BigDecimal proposedPrice,
            BigDecimal purchaseBudget
    ) {
        this.planId = planId;
        this.symbol = symbol;
        this.endDateExclusive = endDateExclusive;
        this.window = window;
        this.plannedAt = plannedAt;
        this.currentPrice = currentPrice;
        this.zScore = zScore;
        this.rollingMean = rollingMean;
        this.rollingStddev = rollingStddev;
        this.signal = signal;
        this.reason = reason;
        this.proposalSide = proposalSide;
        this.proposedQuantity = proposedQuantity;
        this.proposedPrice = proposedPrice;
        this.purchaseBudget = purchaseBudget;
    }
}

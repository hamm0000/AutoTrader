package dev.hamm0000.autotrader.adapter.postgres.strategy.zscore;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ZScoreTradePlanRepository extends JpaRepository<ZScoreTradePlanEntity, UUID> {

    Optional<ZScoreTradePlanEntity>
            findBySymbolAndEndDateExclusiveAndWindowAndPurchaseBudget(
                    String symbol,
                    LocalDate endDateExclusive,
                    int window,
                    BigDecimal purchaseBudget
            );
}

package dev.hamm0000.autotrader.adapter.postgres.strategy.zscore;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ZScoreTradePlanRepository extends JpaRepository<ZScoreTradePlanEntity, UUID> {
}

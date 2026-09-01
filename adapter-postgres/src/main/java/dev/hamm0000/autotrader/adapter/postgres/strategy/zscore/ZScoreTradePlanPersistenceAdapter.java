package dev.hamm0000.autotrader.adapter.postgres.strategy.zscore;

import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.SaveZScoreTradePlan;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.SaveZScoreTradePlanCommand;
import dev.hamm0000.autotrader.trading.strategy.zscore.application.port.out.ZScoreTradePlanSaveException;
import java.util.Objects;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ZScoreTradePlanPersistenceAdapter implements SaveZScoreTradePlan {

    private final ZScoreTradePlanRepository tradePlans;

    ZScoreTradePlanPersistenceAdapter(ZScoreTradePlanRepository tradePlans) {
        this.tradePlans = Objects.requireNonNull(tradePlans, "tradePlans must not be null");
    }

    @Override
    @Transactional
    public void save(SaveZScoreTradePlanCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        try {
            // flush 없이 커밋 시점까지 미루면 중복 키 실패가 이 메서드 밖에서 일어나 아래의 catch가 못 잡습니다
            tradePlans.saveAndFlush(ZScoreTradePlanEntityMapper.toEntity(command));
        } catch (DataAccessException exception) {
            throw new ZScoreTradePlanSaveException(
                    "Z-score 거래 계획을 저장하지 못했습니다",
                    exception
            );
        }
    }
}

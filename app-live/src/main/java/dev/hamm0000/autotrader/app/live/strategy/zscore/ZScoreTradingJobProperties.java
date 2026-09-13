package dev.hamm0000.autotrader.app.live.strategy.zscore;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("autotrader.strategy.zscore.job")
record ZScoreTradingJobProperties(
        int lookbackWindow,
        BigDecimal purchaseBudget,
        List<Target> targets
) {

    ZScoreTradingJobProperties {
        if (targets != null) {
            targets = List.copyOf(targets);
        }
    }

    void validate() {
        if (lookbackWindow < 2) {
            throw new IllegalStateException("Z-score 과거 종가 개수는 2 이상이어야 합니다");
        }
        if (purchaseBudget == null || purchaseBudget.signum() <= 0) {
            throw new IllegalStateException("Z-score 종목별 매수 예산은 0보다 커야 합니다");
        }
        if (targets == null || targets.isEmpty()) {
            throw new IllegalStateException("Z-score 거래 대상 종목이 필요합니다");
        }

        Set<String> symbols = new HashSet<>();
        for (Target target : targets) {
            if (target == null) {
                throw new IllegalStateException("Z-score 거래 대상 종목은 비어 있을 수 없습니다");
            }
            target.validate();
            if (!symbols.add(target.symbol())) {
                throw new IllegalStateException(
                        "Z-score 거래 대상 종목은 중복될 수 없습니다: " + target.symbol()
                );
            }
        }
    }

    record Target(String symbol, String companyName, String sector) {

        void validate() {
            requireText(symbol, "Z-score 거래 대상 종목 코드는 비어 있을 수 없습니다");
            requireText(companyName, "Z-score 거래 대상 회사명은 비어 있을 수 없습니다");
            requireText(sector, "Z-score 거래 대상 업종은 비어 있을 수 없습니다");
        }

        private static void requireText(String value, String message) {
            if (value == null || value.isBlank()) {
                throw new IllegalStateException(message);
            }
        }
    }
}

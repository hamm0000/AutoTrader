package dev.hamm0000.autotrader.adapter.strategy.zscore.http;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.hamm0000.autotrader.trading.strategy.application.port.out.zscore.ZScoreEvaluationException;
import dev.hamm0000.autotrader.trading.strategy.application.port.out.zscore.ZScoreEvaluationResult;
import dev.hamm0000.autotrader.trading.strategy.domain.StrategySignal;

record ZScoreHttpResponse(
        String signal,
        // 외부 응답의 숫자 필드 누락을 null로 보존하여 예외 던지기
        // primitive double이면 누락값과 유효한 0.0을 구분할 수 없음
        @JsonProperty("z_score") Double zScore,
        @JsonProperty("rolling_mean") Double rollingMean,
        @JsonProperty("rolling_stddev") Double rollingStddev,
        String reason
) {

    ZScoreEvaluationResult toResult() {
        if (signal == null || zScore == null || rollingMean == null
                || rollingStddev == null || reason == null) {
            throw invalidResponse("Z-score 전략 응답에 필수 필드가 없습니다");
        }

        StrategySignal strategySignal;
        try {
            strategySignal = StrategySignal.valueOf(signal);
        } catch (IllegalArgumentException exception) {
            throw new ZScoreEvaluationException(
                    ZScoreEvaluationException.Reason.INVALID_RESPONSE,
                    "Z-score 전략 응답의 매매 신호가 올바르지 않습니다",
                    exception
            );
        }

        return new ZScoreEvaluationResult(
                strategySignal,
                zScore,
                rollingMean,
                rollingStddev,
                reason
        );
    }

    private static ZScoreEvaluationException invalidResponse(String message) {
        return new ZScoreEvaluationException(
                ZScoreEvaluationException.Reason.INVALID_RESPONSE,
                message
        );
    }
}

package dev.hamm0000.autotrader.adapter.kis.auth;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

final class TokenIssueRetryPolicy {

    private static final List<Duration> RETRY_DELAYS = List.of(
            Duration.ofMinutes(1),
            Duration.ofMinutes(2),
            Duration.ofMinutes(4),
            Duration.ofMinutes(8),
            Duration.ofMinutes(10));

    Optional<Duration> delayAfter(int consecutiveFailures) {
        if (consecutiveFailures < 1) {
            throw new IllegalArgumentException("연속 실패 횟수는 1 이상이어야 합니다");
        }

        int delayIndex = consecutiveFailures - 1;
        if (delayIndex >= RETRY_DELAYS.size()) {
            return Optional.empty();
        }
        return Optional.of(RETRY_DELAYS.get(delayIndex));
    }
}

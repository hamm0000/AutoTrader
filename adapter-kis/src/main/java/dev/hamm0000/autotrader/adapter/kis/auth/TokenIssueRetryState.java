package dev.hamm0000.autotrader.adapter.kis.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * 토큰 발급 실패 후 자동 재시도 여부를 관리하는 State Machine
 *
 * <pre>
 * Ready
 * - 발급 실패 -> Waiting
 *
 * Waiting
 *   - 재시도 시간 전이면 Waiting을 유지한 채 발급 보류
 *   - 재시도 시간 후 발급 성공 -> Ready
 *   - 재시도 시간 후 발급 실패 -> Waiting or Halted
 *
 * Halted
 *   - 자동 발급을 계속 보류하고 마지막 발급 실패를 유지(수동 개입-TODO:알림기능(app-live))
 * </pre>
 */

final class TokenIssueRetryState {

    private final TokenIssueRetryPolicy policy = new TokenIssueRetryPolicy();

    private State state = Ready.INSTANCE;

    boolean isIssueOnHoldAt(Instant now) {
        return switch (state) {
            case Ready _ -> false;
            case Waiting waiting -> now.isBefore(waiting.retryAt());
            case Halted _ -> true;
        };
    }

    void recordFailure(KisAuthException failure, Instant failedAt) {
        int consecutiveFailures = state.consecutiveFailures() + 1;
        Optional<Duration> retryDelay = policy.delayAfter(consecutiveFailures);

        if (retryDelay.isEmpty()) {
            state = new Halted(consecutiveFailures, failure);
            return;
        }
        state = new Waiting(consecutiveFailures, failedAt.plus(retryDelay.get()), failure);
    }

    void recordSuccess() {
        state = Ready.INSTANCE;
    }

    KisAuthException issueOnHoldFailure() {
        return switch (state) {
            case Waiting waiting -> KisAuthException.retryDeferred(
                    waiting.retryAt(), waiting.lastFailure());
            case Halted halted -> KisAuthException.retryHalted(halted.lastFailure());
            case Ready _ -> throw new IllegalStateException("기록된 토큰 발급 실패가 없습니다");
        };
    }

    private sealed interface State permits Ready, Waiting, Halted {

        int consecutiveFailures();
    }

    private enum Ready implements State {
        INSTANCE;

        @Override
        public int consecutiveFailures() {
            return 0;
        }
    }

    private record Waiting(
            int consecutiveFailures,
            Instant retryAt,
            KisAuthException lastFailure) implements State {
    }

    private record Halted(
            int consecutiveFailures,
            KisAuthException lastFailure) implements State {
    }
}

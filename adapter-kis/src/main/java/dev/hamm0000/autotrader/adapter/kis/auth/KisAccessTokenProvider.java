package dev.hamm0000.autotrader.adapter.kis.auth;

import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.Optional;

public final class KisAccessTokenProvider {

    private static final Duration REFRESH_MARGIN = Duration.ofMinutes(30);

    private final AccessTokenIssuer issuer;
    private final InstantSource timeSource;
    private final TokenIssueRetryPolicy retryPolicy = new TokenIssueRetryPolicy();
    private final Object lock = new Object();

    private AccessToken cached;
    private int consecutiveFailures;
    private Instant retryAt;
    private boolean retryHalted;
    private KisAuthException lastFailure;

    public KisAccessTokenProvider(AccessTokenIssuer issuer, InstantSource timeSource) {
        this.issuer = issuer;
        this.timeSource = timeSource;
    }

    public AccessToken getAccessToken() {
        synchronized (lock) {
            Instant now = timeSource.instant();
            if (cached != null && cached.isUsableAt(now, REFRESH_MARGIN)) {
                return cached;
            }
            if (isIssueOnHoldAt(now)) {
                return fallbackOrThrow(now);
            }
            try {
                cached = issuer.issue();
                resetRetryState();
                return cached;
            } catch (KisAuthException failure) {
                Instant failedAt = timeSource.instant();
                recordFailure(failure, failedAt);
                return fallbackOrThrow(failedAt);
            }
        }
    }
    // backoff 대기 중이거나 자동 재시도를 소진했으면 KIS를 다시 호출하지 않음
    private boolean isIssueOnHoldAt(Instant now) {
        return retryHalted || retryAt != null && now.isBefore(retryAt);
    }

    // 재시도 스케줄이 소진되면 자동 재시도를 중단
    private void recordFailure(KisAuthException failure, Instant failedAt) {
        lastFailure = failure;
        consecutiveFailures++;

        Optional<Duration> retryDelay = retryPolicy.delayAfter(consecutiveFailures);
        if (retryDelay.isEmpty()) {
            retryHalted = true;
            retryAt = null;
            return;
        }
        retryAt = failedAt.plus(retryDelay.get());
    }

    private AccessToken fallbackOrThrow(Instant now) {
        if (cached != null && cached.isValidAt(now)) {
            return cached;
        }
        throw lastFailure;
    }

    // 발급에 성공했을 때만 호출되어 이전 실패 이력을 지움
    private void resetRetryState() {
        consecutiveFailures = 0;
        retryAt = null;
        retryHalted = false;
        lastFailure = null;
    }
}

package dev.hamm0000.autotrader.adapter.kis.auth;

import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;

public final class KisAccessTokenProvider {

    private static final Duration REFRESH_MARGIN = Duration.ofMinutes(30);

    private final AccessTokenIssuer issuer;
    private final InstantSource timeSource;
    // 외부 협력 객체라기 보다는 provider 전용 내부 상태이기 때문에 직접 new
    private final TokenIssueRetryState retryState = new TokenIssueRetryState();
    private final Object lock = new Object();

    private AccessToken cached;

    public KisAccessTokenProvider(AccessTokenIssuer issuer, InstantSource timeSource) {
        this.issuer = issuer;
        this.timeSource = timeSource;
    }

    /**
     * <b>사용할 수 있는 KIS Access Token을 반환합니다.</b>
     *
     * <p>갱신 여유 시간(30분 이상)을 만족하는 캐시 토큰은 재사용하고, 새 발급이 필요하지만
     * 발급이 보류되었거나 발급에 실패한 경우에는, 아직 만료되지 않은 캐시 토큰을 반환합니다.
     *
     * <p>동시에 캐시 미스가 발생해도 토큰 방급 요청이 중복되지 않도록 처리했습니다.
     *
     * @return 사용 가능한 KIS Access Token
     * @throws KisAuthException 새 토큰을 발급할 수 없고, 반환할 수 있는 캐시 토큰도 없을 때
     */
    public AccessToken getAccessToken() {
        synchronized (lock) {
            Instant now = timeSource.instant();

            // 만료 임박 전까지는 기존 토큰을 재사용
            if (cached != null && cached.isUsableAt(now, REFRESH_MARGIN)) {
                return cached;
            }
            // Backoff 중에는 KIS를 호출하지 않음
            if (retryState.isIssueOnHoldAt(now)) {
                return fallbackOrThrow(now, retryState.issueOnHoldFailure());
            }
            try {
                cached = issuer.issue();
                retryState.recordSuccess();
                return cached;
            } catch (KisAuthException failure) {
                Instant failedAt = timeSource.instant();
                retryState.recordFailure(failure, failedAt);
                return fallbackOrThrow(failedAt, failure);
            }
        }
    }

    // 아직 만료되지 않은 캐시 토큰이 있으면 반환하고, 없으면 현재 실패 상태를 전파
    private AccessToken fallbackOrThrow(Instant now, KisAuthException failure) {
        if (cached != null && cached.isValidAt(now)) {
            return cached;
        }
        throw failure;
    }
}

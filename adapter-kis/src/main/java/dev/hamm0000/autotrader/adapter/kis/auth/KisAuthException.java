package dev.hamm0000.autotrader.adapter.kis.auth;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public class KisAuthException extends RuntimeException {

    public enum Reason {
        /** KIS 토큰 발급 요청 실패 */
        ISSUE_FAILED,
        /** Backoff 대기 중이라 새 토큰 발급을 시도하지 않음*/
        RETRY_DEFERRED, // backoff 때문에 요청 거절
        /** 자동 retry 횟수를 소진해 새 토큰 발급을 중단 */
        RETRY_HALTED // retry끝
    }

    private final Reason reason;
    private final Instant retryAt;

    public KisAuthException(String message) {
        this(Reason.ISSUE_FAILED, message, null, null);
    }

    public KisAuthException(String message, Throwable cause) {
        this(Reason.ISSUE_FAILED, message, cause, null);
    }

    private KisAuthException(Reason reason, String message, Throwable cause, Instant retryAt) {
        super(message, cause);
        this.reason = Objects.requireNonNull(reason);
        this.retryAt = retryAt;
    }

    static KisAuthException retryDeferred(Instant retryAt, KisAuthException lastFailure) {
        Objects.requireNonNull(retryAt);
        Objects.requireNonNull(lastFailure);
        return new KisAuthException(
                Reason.RETRY_DEFERRED,
                "KIS 접근 토큰 발급 재시도 대기 중입니다: " + retryAt,
                lastFailure,
                retryAt);
    }

    static KisAuthException retryHalted(KisAuthException lastFailure) {
        Objects.requireNonNull(lastFailure);
        return new KisAuthException(
                Reason.RETRY_HALTED,
                "KIS 접근 토큰 자동 재시도가 중단되었습니다",
                lastFailure,
                null);
    }

    public Reason reason() {
        return reason;
    }

    public Optional<Instant> retryAt() {
        return Optional.ofNullable(retryAt);
    }
}

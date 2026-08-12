package dev.hamm0000.autotrader.adapter.kis.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TokenIssueRetryStateTest {

    private static final Instant NOW = Instant.parse("2026-08-01T00:00:00Z");

    private final TokenIssueRetryState retryState = new TokenIssueRetryState();

    @Test
    void 발급에_실패하면_재시도_시각_전까지_발급을_보류한다() {
        // given
        KisAuthException failure = new KisAuthException("발급 실패");
        retryState.recordFailure(failure, NOW);

        // when & then
        assertThat(retryState.isIssueOnHoldAt(NOW.plusSeconds(59))).isTrue();
        assertThat(retryState.issueOnHoldFailure())
                .satisfies(exception -> {
                    assertThat(exception).hasMessageContaining("재시도 대기");
                    assertThat(exception.reason()).isEqualTo(KisAuthException.Reason.RETRY_DEFERRED);
                    assertThat(exception.retryAt()).contains(NOW.plus(Duration.ofMinutes(1)));
                    assertThat(exception).hasCause(failure);
                });
        assertThat(retryState.isIssueOnHoldAt(NOW.plus(Duration.ofMinutes(1)))).isFalse();
    }

    @Test
    void 마지막_재시도까지_실패하면_시간이_지나도_발급을_보류한다() {
        // given
        KisAuthException lastFailure = new KisAuthException("마지막 발급 실패");
        retryState.recordFailure(new KisAuthException("발급 실패"), NOW);
        retryState.recordFailure(new KisAuthException("발급 실패"), NOW.plus(Duration.ofMinutes(1)));
        retryState.recordFailure(new KisAuthException("발급 실패"), NOW.plus(Duration.ofMinutes(3)));
        retryState.recordFailure(new KisAuthException("발급 실패"), NOW.plus(Duration.ofMinutes(7)));
        retryState.recordFailure(new KisAuthException("발급 실패"), NOW.plus(Duration.ofMinutes(15)));
        retryState.recordFailure(lastFailure, NOW.plus(Duration.ofMinutes(25)));

        // when & then
        assertThat(retryState.isIssueOnHoldAt(NOW.plus(Duration.ofDays(1)))).isTrue();
        assertThat(retryState.issueOnHoldFailure())
                .satisfies(exception -> {
                    assertThat(exception).hasMessageContaining("자동 재시도가 중단");
                    assertThat(exception.reason()).isEqualTo(KisAuthException.Reason.RETRY_HALTED);
                    assertThat(exception.retryAt()).isEmpty();
                    assertThat(exception).hasCause(lastFailure);
                });
    }

    @Test
    void 발급에_성공하면_실패_상태를_초기화한다() {
        // given
        retryState.recordFailure(new KisAuthException("발급 실패"), NOW);

        // when
        retryState.recordSuccess();

        // then
        assertThat(retryState.isIssueOnHoldAt(NOW)).isFalse();
    }
}

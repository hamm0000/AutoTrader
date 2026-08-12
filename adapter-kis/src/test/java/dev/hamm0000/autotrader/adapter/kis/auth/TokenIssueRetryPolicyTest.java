package dev.hamm0000.autotrader.adapter.kis.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TokenIssueRetryPolicyTest {

    private final TokenIssueRetryPolicy policy = new TokenIssueRetryPolicy();

    @ParameterizedTest
    @MethodSource("retrySchedule")
    void 연속으로_실패하면_지정된_간격_뒤에_재시도_대기_시간을_반환한다(int consecutiveFailures, Duration expectedDelay) {
        assertThat(policy.delayAfter(consecutiveFailures)).contains(expectedDelay);
    }

    private static Stream<Arguments> retrySchedule() {
        return Stream.of(
                Arguments.of(1, Duration.ofMinutes(1)),
                Arguments.of(2, Duration.ofMinutes(2)),
                Arguments.of(3, Duration.ofMinutes(4)),
                Arguments.of(4, Duration.ofMinutes(8)),
                Arguments.of(5, Duration.ofMinutes(10)));
    }

    @Test
    void 마지막_재시도까지_실패하면_자동_재시도를_종료한다() {
        assertThat(policy.delayAfter(6)).isEmpty();
    }
}

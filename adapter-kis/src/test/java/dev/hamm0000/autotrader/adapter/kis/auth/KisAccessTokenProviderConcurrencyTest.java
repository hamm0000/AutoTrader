package dev.hamm0000.autotrader.adapter.kis.auth;

import static org.assertj.core.api.Assertions.assertThat;

import dev.hamm0000.autotrader.adapter.kis.auth.testdouble.BlockingFakeIssuer;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class KisAccessTokenProviderConcurrencyTest {

    private static final Instant NOW = Instant.parse("2026-08-01T00:00:00Z");

    /**
     *
     * 이 동시성 테스트 한계점: 동시성 문제가 없다고는 보장할 수 없다.
     * 첫 발급을 캐시 저장 전 상태에서 멈춰 경합 가능성을 높이긴 했지만,
     * 다른 모든 스레드가 실제로 Provider 내부의 동일한 캐시 미스 구간까지 진입했는지는 보장하지 못한다.
     *
     * 따라서 해당 테스트는 동시 요청 환경에서 최종적으로 발급이 한 번만 발생하는지를 검증한다.
     */

    @Test
    void 캐시_미스_상태에서_동시_요청이_발생해도_토큰은_한_번만_발급한다() throws Exception {
        // given
        int threadCount = 8;
        AccessToken issuedToken = new AccessToken("issued-value", NOW.plus(Duration.ofHours(24)));
        BlockingFakeIssuer issuer = new BlockingFakeIssuer(issuedToken);
        KisAccessTokenProvider provider = new KisAccessTokenProvider(issuer, InstantSource.fixed(NOW));

        CyclicBarrier startTogether = new CyclicBarrier(threadCount);
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        try {
            // when
            List<Future<AccessToken>> futures = IntStream.range(0, threadCount)
                    .mapToObj(ignored -> pool.submit(() -> {
                        startTogether.await(); //ready
                        return provider.getAccessToken();
                    }))
                    .toList();

            // 첫 발급 시작을 확인
            // 발급 완료는 아래 completeIssue 호출 전까지 보류
            assertThat(issuer.awaitIssueStarted(2, TimeUnit.SECONDS)).isTrue();


            issuer.completeIssue();

            // then
            for (Future<AccessToken> future : futures) {
                assertThat(future.get(2, TimeUnit.SECONDS)).isSameAs(issuedToken);
            }
            assertThat(issuer.issueCount()).isEqualTo(1);
        } finally {
            issuer.completeIssue(); // 조기 실패해도 대기 중인 스레드를 풀어줌
            pool.shutdownNow();
        }
    }
}

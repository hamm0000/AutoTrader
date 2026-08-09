package dev.hamm0000.autotrader.adapter.kis.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import dev.hamm0000.autotrader.adapter.kis.auth.testdouble.BlockingFakeIssuer;
import dev.hamm0000.autotrader.adapter.kis.auth.testdouble.FakeInstantSource;
import dev.hamm0000.autotrader.adapter.kis.auth.testdouble.FakeIssuer;
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


public class KisAccessTokenProviderTest {

    private static final Instant NOW = Instant.parse("2026-08-01T00:00:00Z");

    @Test
    void 토큰이_없으면_새로_발급받는다() {
        // given
        // 캐시 없어서 시간 안 중요함
        AccessToken issuedToken = new AccessToken("issued-value", NOW);
        FakeIssuer issuer = new FakeIssuer(issuedToken);
        KisAccessTokenProvider provider = new KisAccessTokenProvider(issuer, InstantSource.fixed(NOW));

        // when
        AccessToken token = provider.getAccessToken();

        // then
        assertThat(token).isSameAs(issuedToken);
        assertThat(issuer.issueCount()).isEqualTo(1);
    }

    @Test
    void 유효한_토큰이_있으면_발급을_요청하지_않고_재사용_한다() {
        // given
        AccessToken issuedToken = new AccessToken("issued-value", NOW.plus(Duration.ofHours(24)));
        FakeIssuer issuer = new FakeIssuer(issuedToken);
        FakeInstantSource timeSource = new FakeInstantSource(NOW);
        KisAccessTokenProvider provider = new KisAccessTokenProvider(issuer, timeSource);

        // when
        AccessToken first = provider.getAccessToken();
        AccessToken second = provider.getAccessToken();

        // then
        assertThat(second).isSameAs(first);
        assertThat(issuer.issueCount()).isEqualTo(1);
    }

    @Test
    void 만료가_30분_미만_남은_토큰이면_새로_발급받는다() {
        // given
        AccessToken expiring = new AccessToken("expiring-value", NOW.plus(Duration.ofMinutes(29)));
        AccessToken renewed = new AccessToken("renewed-value", NOW.plus(Duration.ofHours(24)));
        FakeIssuer issuer = new FakeIssuer(expiring, renewed);
        FakeInstantSource timeSource = new FakeInstantSource(NOW);
        KisAccessTokenProvider provider = new KisAccessTokenProvider(issuer, timeSource);
        provider.getAccessToken();

        // when
        AccessToken token = provider.getAccessToken();

        // then
        assertThat(token).isSameAs(renewed);
        assertThat(issuer.issueCount()).isEqualTo(2);
    }

    @Test
    void 만료된_토큰이면_새로_발급받는다() {
        // given
        AccessToken expiring = new AccessToken("expiring-value", NOW.plus(Duration.ofHours(1)));
        AccessToken renewed = new AccessToken("renewed-value", NOW.plus(Duration.ofHours(25)));
        FakeIssuer issuer = new FakeIssuer(expiring, renewed);
        FakeInstantSource timeSource = new FakeInstantSource(NOW);
        KisAccessTokenProvider provider = new KisAccessTokenProvider(issuer, timeSource);
        provider.getAccessToken();

        // when
        timeSource.moveTo(NOW.plus(Duration.ofHours(2)));
        AccessToken token = provider.getAccessToken();

        // then
        assertThat(token).isSameAs(renewed);
        assertThat(issuer.issueCount()).isEqualTo(2);
    }

    @Test
    void 발급이_실패하면_예외를_그대로_전파한다() {
        // given
        RuntimeException failure = new RuntimeException("발급 실패");
        AccessTokenIssuer issuer = () -> {
            throw failure;
        };
        KisAccessTokenProvider provider = new KisAccessTokenProvider(issuer, InstantSource.fixed(NOW));

        // when & then
        assertThatThrownBy(provider::getAccessToken)
                .isSameAs(failure);
    }

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

    @Test
    void 만료_임박_토큰의_갱신에_실패해도_실제_만료_전이면_기존_토큰을_반환한다() {
        // given
        AccessToken expiring = new AccessToken("expiring-value", NOW.plus(Duration.ofMinutes(29)));
        FakeIssuer issuer = new FakeIssuer(expiring);
        KisAccessTokenProvider provider = new KisAccessTokenProvider(issuer, InstantSource.fixed(NOW));
        provider.getAccessToken();
        issuer.failWith(new KisAuthException("발급 실패"));

        // when
        AccessToken fallback = provider.getAccessToken();

        // then
        assertThat(fallback).isSameAs(expiring);
        assertThat(issuer.issueCount()).isEqualTo(2);
    }
}

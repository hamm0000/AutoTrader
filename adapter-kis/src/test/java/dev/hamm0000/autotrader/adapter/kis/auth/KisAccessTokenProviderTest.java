package dev.hamm0000.autotrader.adapter.kis.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import dev.hamm0000.autotrader.adapter.kis.auth.testdouble.FakeInstantSource;
import dev.hamm0000.autotrader.adapter.kis.auth.testdouble.FakeIssuer;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/*
1. Token 최초 발급 (Token 미존재 시)
    Token이 없을 때, Token을 요청하면, KIS에서 새 Token을 발급받는다.

2. Token 재사용 및 갱신 (Token 존재 시)
    유효한 Token이 있을 때, Token을 요청하면, KIS를 다시 호출하지 않고 기존 Token을 반환.
    Token이 만료됐거나 만료 임박했을 때, Token을 요청하면, KIS에서 새 Token을 발급받는다.
        이때 새 토큰 발급에 성공한 경우에만 기존 캐시를 교체함.
3. Token 발급 실패
    KIS가 에러를 반환하면, 예외를 던진다.
*/

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

    @Test
    @Disabled("retry 정책 확정 전까지 보류")
    void 갱신에_실패하면_기존_캐시를_유지한다() {
        // 만료 30분 미만 토큰이 있을 때, 갱신에 실패한다면 기존의 캐시를 유지하는 것을 테스트 하려했으나
        // AccessToken의 isUsableAt 때문에 계속 재발급 시도하게 됨.
        // 따라서 나중에 retry정책을 정하고 다시 생각해 보겠음
    }
}

package dev.hamm0000.autotrader.adapter.kis.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import dev.hamm0000.autotrader.adapter.kis.auth.testdouble.FakeInstantSource;
import dev.hamm0000.autotrader.adapter.kis.auth.testdouble.FakeIssuer;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
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

    @Test
    void 갱신_실패_후_backoff_중에는_발급을_요청하지_않고_기존_토큰을_반환한다() {
        // given
        AccessToken expiring = new AccessToken("expiring-value", NOW.plus(Duration.ofMinutes(29)));
        FakeIssuer issuer = new FakeIssuer(expiring);
        FakeInstantSource timeSource = new FakeInstantSource(NOW);
        KisAccessTokenProvider provider = new KisAccessTokenProvider(issuer, timeSource);
        provider.getAccessToken();
        issuer.failWith(new KisAuthException("발급 실패"));
        provider.getAccessToken();

        // when
        timeSource.moveTo(NOW.plusSeconds(59));
        AccessToken fallback = provider.getAccessToken();

        // then
        assertThat(fallback).isSameAs(expiring);
        assertThat(issuer.issueCount()).isEqualTo(2);
    }

    @Test
    void backoff가_끝나면_다시_갱신을_요청한다() {
        // given
        AccessToken expiring = new AccessToken("expiring-value", NOW.plus(Duration.ofMinutes(29)));
        AccessToken renewed = new AccessToken("renewed-value", NOW.plus(Duration.ofHours(24)));
        FakeIssuer issuer = new FakeIssuer(expiring, renewed);
        FakeInstantSource timeSource = new FakeInstantSource(NOW);
        KisAccessTokenProvider provider = new KisAccessTokenProvider(issuer, timeSource);
        provider.getAccessToken();
        issuer.failWith(new KisAuthException("발급 실패"));
        provider.getAccessToken();

        // when
        timeSource.moveTo(NOW.plus(Duration.ofMinutes(1)));
        issuer.recover();
        AccessToken token = provider.getAccessToken();

        // then
        assertThat(token).isSameAs(renewed);
        assertThat(issuer.issueCount()).isEqualTo(3);
    }

}

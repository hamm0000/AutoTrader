package dev.hamm0000.autotrader.adapter.kis.auth;

import static org.assertj.core.api.Assertions.assertThat;

import dev.hamm0000.autotrader.adapter.kis.auth.mock.FakeInstantSource;
import dev.hamm0000.autotrader.adapter.kis.auth.mock.FakeIssuer;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
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

    @Test
    void 토큰이_없으면_새로_발급받는다() {
        // given
        // 캐시 없어서 시간 안 중요함
        AccessToken issuedToken = new AccessToken("issued-value", Instant.now());
        FakeIssuer issuer = new FakeIssuer(issuedToken);
        KisAccessTokenProvider provider = new KisAccessTokenProvider(issuer, InstantSource.system());

        // when
        AccessToken token = provider.getAccessToken();

        // then
        assertThat(token).isSameAs(issuedToken);
        assertThat(issuer.issueCount()).isEqualTo(1);
    }

    @Test
    void 유효한_토큰이_있으면_발급을_요청하지_않고_재사용_한다() {
        // given
        Instant now = Instant.now();
        AccessToken issuedToken = new AccessToken("issued-value", now.plus(Duration.ofHours(24)));
        FakeIssuer issuer = new FakeIssuer(issuedToken);
        FakeInstantSource timeSource = new FakeInstantSource(now);
        KisAccessTokenProvider provider = new KisAccessTokenProvider(issuer, timeSource);

        // when
        AccessToken first = provider.getAccessToken();
        AccessToken second = provider.getAccessToken();

        // then
        assertThat(second).isSameAs(first);
        assertThat(issuer.issueCount()).isEqualTo(1);
    }

    @Test
    void 만료된_토큰이면_새로_발급받는다() {
        // given
        Instant now = Instant.now();
        AccessToken expiring = new AccessToken("expiring-value", now.plus(Duration.ofHours(1)));
        AccessToken renewed = new AccessToken("renewed-value", now.plus(Duration.ofHours(25)));
        FakeIssuer issuer = new FakeIssuer(expiring, renewed);
        FakeInstantSource timeSource = new FakeInstantSource(now);
        KisAccessTokenProvider provider = new KisAccessTokenProvider(issuer, timeSource);
        provider.getAccessToken();

        // when
        timeSource.moveTo(now.plus(Duration.ofHours(2)));
        AccessToken token = provider.getAccessToken();

        // then
        assertThat(token).isSameAs(renewed);
        assertThat(issuer.issueCount()).isEqualTo(2);
    }
}

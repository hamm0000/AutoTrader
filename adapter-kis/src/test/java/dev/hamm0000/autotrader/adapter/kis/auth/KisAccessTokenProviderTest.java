package dev.hamm0000.autotrader.adapter.kis.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import dev.hamm0000.autotrader.adapter.kis.auth.mock.FakeIssuer;
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
        AccessToken issuedToken = new AccessToken("issued-value", Instant.now());
        FakeIssuer issuer = new FakeIssuer(issuedToken);
        KisAccessTokenProvider provider = new KisAccessTokenProvider(issuer);

        // when
        AccessToken token = provider.getAccessToken();

        // then
        assertThat(token).isSameAs(issuedToken);
        assertThat(issuer.issueCount()).isEqualTo(1);
    }

    @Test
    void 유효한_토큰이_있으면_발급을_요청하지_않고_재사용_한다() {
        // given
        AccessToken issuedToken = new AccessToken("issued-value", Instant.now());
        FakeIssuer issuer = new FakeIssuer(issuedToken);
        KisAccessTokenProvider provider = new KisAccessTokenProvider(issuer);

        // when
        AccessToken first = provider.getAccessToken();
        AccessToken second = provider.getAccessToken();

        // then
        assertThat(second).isSameAs(first);
        assertThat(issuer.issueCount()).isEqualTo(1);
    }

    @Test
    void 만료된_토큰이면_새로_발급받는다() {

    }


}

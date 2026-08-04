package dev.hamm0000.autotrader.adapter.kis.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import org.junit.jupiter.api.Test;

class KisTokenResponseTest {

    private static final Instant NOW = Instant.parse("2026-08-01T00:00:00Z");

    @Test
    void 유효기간을_현재_시각에_더한_만료_시각을_가진_토큰을_돌려준다() {
        // given
        KisTokenResponse response = new KisTokenResponse("issued-token-value", 86400L); //24h

        // when
        AccessToken token = response.toAccessToken(InstantSource.fixed(NOW));

        // then
        assertThat(token.value()).isEqualTo("issued-token-value");
        assertThat(token.expiresAt()).isEqualTo(NOW.plus(Duration.ofHours(24)));
    }

    @Test
    void 접근_토큰이_없으면_예외를_던진다() {
        // given
        KisTokenResponse response = new KisTokenResponse(null, 86400L);

        // when & then
        assertThatThrownBy(() -> response.toAccessToken(InstantSource.fixed(NOW)))
                .isInstanceOf(KisAuthException.class);
    }

    @Test
    void 유효기간이_없으면_예외를_던진다() {
        // given
        KisTokenResponse response = new KisTokenResponse("issued-token-value", null);

        // when & then
        assertThatThrownBy(() -> response.toAccessToken(InstantSource.fixed(NOW)))
                .isInstanceOf(KisAuthException.class);
    }
}

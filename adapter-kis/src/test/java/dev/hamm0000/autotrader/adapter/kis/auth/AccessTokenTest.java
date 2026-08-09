package dev.hamm0000.autotrader.adapter.kis.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;


class AccessTokenTest {

    private static final Duration REFRESH_MARGIN = Duration.ofMinutes(30);
    private static final Instant EXPIRES_AT = Instant.now();

    @Test
    void 만료가_30분_이상_남았다면_사용할_수_있다() {
        // given
        AccessToken token = new AccessToken("issued-value", EXPIRES_AT);

        // when
        boolean usable = token.isUsableAt(EXPIRES_AT.minus(REFRESH_MARGIN), REFRESH_MARGIN);

        // then
        assertThat(usable).isTrue();
    }

    @Test
    void 만료가_30분_미만_남았다면_사용할_수_없다() {
        // given
        AccessToken token = new AccessToken("issued-value", EXPIRES_AT);

        // when
        boolean usable = token.isUsableAt(EXPIRES_AT.minus(REFRESH_MARGIN).plusSeconds(1), REFRESH_MARGIN);

        // then
        assertThat(usable).isFalse();
    }

    @Test
    void 만료되었다면_사용할_수_없다() {
        // given
        AccessToken token = new AccessToken("issued-value", EXPIRES_AT);

        // when
        boolean usable = token.isUsableAt(EXPIRES_AT, REFRESH_MARGIN);

        // then
        assertThat(usable).isFalse();
    }

    @Test
    void 만료_전이면_유효하다() {
        // given
        AccessToken token = new AccessToken("issued-value", EXPIRES_AT);

        // when
        boolean valid = token.isValidAt(EXPIRES_AT.minusSeconds(1));

        // then
        assertThat(valid).isTrue();
    }

    @Test
    void 만료_시각부터는_유효하지_않다() {
        // given
        AccessToken token = new AccessToken("issued-value", EXPIRES_AT);

        // when
        boolean valid = token.isValidAt(EXPIRES_AT);

        // then
        assertThat(valid).isFalse();
    }
}

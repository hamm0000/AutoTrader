package dev.hamm0000.autotrader.adapter.kis.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.InstantSource;

public record KisTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("expires_in") Long expiresIn) {

    public AccessToken toAccessToken(InstantSource timeSource) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new KisAuthException("KIS 토큰 응답에 access_token이 없습니다");
        }
        if (expiresIn == null || expiresIn <= 0) {
            throw new KisAuthException("KIS 토큰 응답에 expires_in이 없습니다");
        }
        return new AccessToken(accessToken, timeSource.instant().plusSeconds(expiresIn));
    }
}

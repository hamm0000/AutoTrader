package dev.hamm0000.autotrader.adapter.kis.auth;

import java.time.InstantSource;

public final class KisAccessTokenProvider {

    private final AccessTokenIssuer issuer;
    private final InstantSource timeSource;

    private AccessToken cached;

    public KisAccessTokenProvider(AccessTokenIssuer issuer, InstantSource timeSource) {
        this.issuer = issuer;
        this.timeSource = timeSource;
    }

    public AccessToken getAccessToken() {
        if (cached != null && timeSource.instant().isBefore(cached.expiresAt())) {
            return cached;
        }
        cached = issuer.issue();
        return cached;
    }
}

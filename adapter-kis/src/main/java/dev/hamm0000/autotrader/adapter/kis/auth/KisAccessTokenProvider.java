package dev.hamm0000.autotrader.adapter.kis.auth;

import java.time.Duration;
import java.time.InstantSource;

public final class KisAccessTokenProvider {

    private static final Duration REFRESH_MARGIN = Duration.ofMinutes(30);

    private final AccessTokenIssuer issuer;
    private final InstantSource timeSource;

    private AccessToken cached;

    public KisAccessTokenProvider(AccessTokenIssuer issuer, InstantSource timeSource) {
        this.issuer = issuer;
        this.timeSource = timeSource;
    }

    public AccessToken getAccessToken() {
        if (cached != null && cached.isUsableAt(timeSource.instant(), REFRESH_MARGIN)) {
            return cached;
        }
        cached = issuer.issue();
        return cached;
    }
}

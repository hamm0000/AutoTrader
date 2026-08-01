package dev.hamm0000.autotrader.adapter.kis.auth;

public final class KisAccessTokenProvider {

    private final AccessTokenIssuer issuer;

    private AccessToken cached;

    public KisAccessTokenProvider(AccessTokenIssuer issuer) {
        this.issuer = issuer;
    }

    public AccessToken getAccessToken() {
        if (cached != null) {
            return cached;
        }
        cached = issuer.issue();
        return cached;
    }
}

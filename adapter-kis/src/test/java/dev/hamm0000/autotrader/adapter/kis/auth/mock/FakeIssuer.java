package dev.hamm0000.autotrader.adapter.kis.auth.mock;

import dev.hamm0000.autotrader.adapter.kis.auth.AccessToken;
import dev.hamm0000.autotrader.adapter.kis.auth.AccessTokenIssuer;

public class FakeIssuer implements AccessTokenIssuer {

    private final AccessToken token;
    private int issueCount;

    public FakeIssuer(AccessToken token) {
        this.token = token;
    }

    @Override
    public AccessToken issue() {
        issueCount++;
        return token;
    }

    public int issueCount() {
        return issueCount;
    }
}

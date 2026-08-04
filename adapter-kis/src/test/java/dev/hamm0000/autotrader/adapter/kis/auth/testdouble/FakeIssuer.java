package dev.hamm0000.autotrader.adapter.kis.auth.testdouble;

import dev.hamm0000.autotrader.adapter.kis.auth.AccessToken;
import dev.hamm0000.autotrader.adapter.kis.auth.AccessTokenIssuer;
import java.util.List;

public class FakeIssuer implements AccessTokenIssuer {

    private final List<AccessToken> tokens;
    private int issueCount;

    public FakeIssuer(AccessToken... tokens) {
        this.tokens = List.of(tokens);
    }

    @Override
    public AccessToken issue() {
        AccessToken token = tokens.get(issueCount);
        issueCount++;
        return token;
    }

    public int issueCount() {
        return issueCount;
    }
}

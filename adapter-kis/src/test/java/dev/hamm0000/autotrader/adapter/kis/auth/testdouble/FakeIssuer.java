package dev.hamm0000.autotrader.adapter.kis.auth.testdouble;

import dev.hamm0000.autotrader.adapter.kis.auth.AccessToken;
import dev.hamm0000.autotrader.adapter.kis.auth.AccessTokenIssuer;
import java.util.List;

public class FakeIssuer implements AccessTokenIssuer {

    private final List<AccessToken> tokens;
    private int tokenIndex;
    private int issueCount;
    private RuntimeException failure;

    public FakeIssuer(AccessToken... tokens) {
        this.tokens = List.of(tokens);
    }

    @Override
    public AccessToken issue() {
        issueCount++;
        if (failure != null) {
            throw failure;
        }
        return tokens.get(tokenIndex++);
    }

    public void failWith(RuntimeException failure) {
        this.failure = failure;
    }

    public int issueCount() {
        return issueCount;
    }
}

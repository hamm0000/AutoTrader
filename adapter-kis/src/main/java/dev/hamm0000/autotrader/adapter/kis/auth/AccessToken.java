package dev.hamm0000.autotrader.adapter.kis.auth;

import java.time.Duration;
import java.time.Instant;

public record AccessToken(String value, Instant expiresAt) {

    public boolean isUsableAt(Instant now, Duration refreshMargin) {
        return !now.isAfter(expiresAt.minus(refreshMargin));
    }
}

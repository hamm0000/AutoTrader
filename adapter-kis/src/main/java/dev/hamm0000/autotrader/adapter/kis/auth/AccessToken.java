package dev.hamm0000.autotrader.adapter.kis.auth;

import java.time.Instant;

public record AccessToken(String value, Instant expiresAt) {
}

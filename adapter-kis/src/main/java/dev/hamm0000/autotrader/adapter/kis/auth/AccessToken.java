package dev.hamm0000.autotrader.adapter.kis.auth;

import java.time.Duration;
import java.time.Instant;

public record AccessToken(String value, Instant expiresAt) {

    // 갱신 없이 그대로 써도 되는지
    public boolean isUsableAt(Instant now, Duration refreshMargin) {
        return !now.isAfter(expiresAt.minus(refreshMargin));
    }
    // 만료 시각을 넘겼는지만 판단(갱신 대상이지만 아직 쓸 수 있는 구간 파악)
    public boolean isValidAt(Instant now) {
        return now.isBefore(expiresAt);
    }
}

package dev.hamm0000.autotrader.trading.order.domain;

import java.util.Objects;

/**
 * 동일한 매매 판단으로 로컬 Order가 중복 생성되는 것을 방지하는 키입니다.
 * <br/>
 * 브로커 주문 제출의 멱등성을 보장하지 않습니다.
 */
public record OrderIdempotencyKey(String value) {

    public OrderIdempotencyKey {
        Objects.requireNonNull(value, "value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }
}

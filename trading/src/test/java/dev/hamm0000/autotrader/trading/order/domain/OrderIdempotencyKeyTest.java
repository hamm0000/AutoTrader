package dev.hamm0000.autotrader.trading.order.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class OrderIdempotencyKeyTest {

    @Test
    void 주문_멱등키는_공백이_아닌_값을_보존한다() {
        OrderIdempotencyKey key = new OrderIdempotencyKey("zscore:005930:2026-09-02");

        assertEquals("zscore:005930:2026-09-02", key.value());
    }

    @Test
    void 주문_멱등키는_공백일_수_없다() {
        assertThrows(IllegalArgumentException.class, () -> new OrderIdempotencyKey("  "));
    }
}

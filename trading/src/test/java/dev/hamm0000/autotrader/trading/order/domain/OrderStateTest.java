package dev.hamm0000.autotrader.trading.order.domain;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class OrderStateTest {

    private static final Instant OCCURRED_AT = Instant.parse("2026-09-05T00:10:01Z");

    @Test
    void 접수된_주문에는_브로커_주문번호가_필요하다() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OrderState.Submitted(" ", OCCURRED_AT)
        );
    }

    @Test
    void 거절된_주문에는_사유가_필요하다() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OrderState.Rejected(" ", OCCURRED_AT)
        );
    }

    @Test
    void 결과를_확인하지_못한_주문에는_사유가_필요하다() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OrderState.SubmissionUnknown(" ", OCCURRED_AT)
        );
    }

    @Test
    void 모든_상태에는_발생시각이_필요하다() {
        assertThrows(
                NullPointerException.class,
                () -> new OrderState.PendingSubmission(null)
        );
    }
}

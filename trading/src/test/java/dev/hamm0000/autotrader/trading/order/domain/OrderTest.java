package dev.hamm0000.autotrader.trading.order.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.hamm0000.autotrader.trading.proposal.domain.BuyProposal;
import dev.hamm0000.autotrader.trading.proposal.domain.SellProposal;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderTest {

    private static final OrderId ORDER_ID = new OrderId(
            UUID.fromString("63df7894-b456-4bf7-85cd-706d9d32c7c9")
    );
    private static final OrderIdempotencyKey IDEMPOTENCY_KEY = new OrderIdempotencyKey(
            "zscore:005930:2026-09-02"
    );
    private static final Instant PREPARED_AT = Instant.parse("2026-09-05T00:10:00Z");
    private static final Instant RESOLVED_AT = Instant.parse("2026-09-05T00:10:01Z");

    @Test
    void 매수_제안으로_ZSCORE_평가가격을_기록한_시장가_매수_주문을_준비한다() {
        // when
        Order order = Order.prepare(
                ORDER_ID,
                IDEMPOTENCY_KEY,
                new BuyProposal(
                        "005930",
                        138,
                        new BigDecimal("72300"),
                        new BigDecimal("10000000")
                ),
                PREPARED_AT
        );

        // then
        assertAll(
                () -> assertEquals(ORDER_ID, order.id()),
                () -> assertEquals(IDEMPOTENCY_KEY, order.idempotencyKey()),
                () -> assertEquals("005930", order.symbol()),
                () -> assertEquals(OrderSide.BUY, order.side()),
                () -> assertEquals(138, order.quantity()),
                () -> assertEquals(OrderType.MARKET, order.type()),
                () -> assertEquals(new BigDecimal("72300"), order.referencePrice()),
                () -> assertEquals(
                        new OrderState.PendingSubmission(PREPARED_AT),
                        order.state()
                ),
                () -> assertEquals(PREPARED_AT, order.preparedAt()),
                () -> assertEquals(PREPARED_AT, order.updatedAt())
        );
    }

    @Test
    void 매도_제안으로_시장가_매도_주문을_준비하며_평가_기준가를_보존한다() {
        // when
        Order order = Order.prepare(
                ORDER_ID,
                IDEMPOTENCY_KEY,
                new SellProposal("005930", 7, new BigDecimal("72300")),
                PREPARED_AT
        );

        // then
        assertAll(
                () -> assertEquals(OrderSide.SELL, order.side()),
                () -> assertEquals(7, order.quantity()),
                () -> assertEquals(OrderType.MARKET, order.type()),
                () -> assertEquals(new BigDecimal("72300"), order.referencePrice())
        );
    }

    @Test
    void 브로커가_주문을_접수하면_브로커_주문번호와_함께_SUBMITTED가_된다() {
        // when
        Order submitted = pendingOrder().markSubmitted("0000123456", RESOLVED_AT);

        // then
        assertAll(
                () -> assertEquals(
                        new OrderState.Submitted("0000123456", RESOLVED_AT),
                        submitted.state()
                ),
                () -> assertEquals(RESOLVED_AT, submitted.updatedAt())
        );
    }

    @Test
    void 브로커가_주문을_거절하면_사유와_함께_REJECTED가_된다() {
        // when
        Order rejected = pendingOrder().markRejected("주문 가능 금액 부족", RESOLVED_AT);

        // then
        assertAll(
                () -> assertEquals(
                        new OrderState.Rejected("주문 가능 금액 부족", RESOLVED_AT),
                        rejected.state()
                ),
                () -> assertEquals(RESOLVED_AT, rejected.updatedAt())
        );
    }

    @Test
    void 응답을_확정할_수_없으면_재주문하지_않도록_SUBMISSION_UNKNOWN이_된다() {
        // when
        Order unknown = pendingOrder().markSubmissionUnknown("응답 수신 전 연결 종료", RESOLVED_AT);

        // then
        assertEquals(
                new OrderState.SubmissionUnknown("응답 수신 전 연결 종료", RESOLVED_AT),
                unknown.state()
        );
    }

    @Test
    void RECONCILIATION으로_접수를_확인하면_UNKNOWN을_SUBMITTED로_확정할_수_있다() {
        // given
        Order unknown = pendingOrder().markSubmissionUnknown(
                "응답 수신 전 연결 종료",
                RESOLVED_AT
        );

        // when
        Order submitted = unknown.markSubmitted(
                "0000123456",
                RESOLVED_AT.plusSeconds(1)
        );

        // then
        assertEquals(
                new OrderState.Submitted("0000123456", RESOLVED_AT.plusSeconds(1)),
                submitted.state()
        );
    }

    @Test
    void RECONCILIATION으로_미접수를_확인하면_UNKNOWN을_REJECTED로_확정할_수_있다() {
        // given
        Order unknown = pendingOrder().markSubmissionUnknown(
                "응답 수신 전 연결 종료",
                RESOLVED_AT
        );

        // when
        Order rejected = unknown.markRejected(
                "KIS 주문 내역에서 확인되지 않음",
                RESOLVED_AT.plusSeconds(1)
        );

        // then
        assertEquals(
                new OrderState.Rejected(
                        "KIS 주문 내역에서 확인되지 않음",
                        RESOLVED_AT.plusSeconds(1)
                ),
                rejected.state()
        );
    }

    @Test
    void 이미_확정된_주문은_다른_제출_결과로_바꿀_수_없다() {
        // given
        Order submitted = pendingOrder().markSubmitted("0000123456", RESOLVED_AT);

        // when & then
        assertThrows(
                IllegalStateException.class,
                () -> submitted.markRejected("뒤늦은 거절", RESOLVED_AT.plusSeconds(1))
        );
    }

    @Test
    void 이전_상태보다_과거_시각으로_상태를_변경할_수_없다() {
        assertThrows(
                IllegalArgumentException.class,
                () -> pendingOrder().markSubmitted(
                        "0000123456",
                        PREPARED_AT.minusSeconds(1)
                )
        );
    }

    private static Order pendingOrder() {
        return Order.prepare(
                ORDER_ID,
                IDEMPOTENCY_KEY,
                new BuyProposal(
                        "005930",
                        138,
                        new BigDecimal("72300"),
                        new BigDecimal("10000000")
                ),
                PREPARED_AT
        );
    }
}

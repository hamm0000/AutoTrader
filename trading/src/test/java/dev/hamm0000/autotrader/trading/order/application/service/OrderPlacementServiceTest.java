package dev.hamm0000.autotrader.trading.order.application.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.hamm0000.autotrader.trading.order.application.port.in.OrderPlacementCommand;
import dev.hamm0000.autotrader.trading.order.application.port.out.OrderAcknowledgment;
import dev.hamm0000.autotrader.trading.order.application.port.out.OrderSaveException;
import dev.hamm0000.autotrader.trading.order.application.port.out.OrderSubmissionException;
import dev.hamm0000.autotrader.trading.order.application.service.testdouble.FakeOrderStore;
import dev.hamm0000.autotrader.trading.order.application.service.testdouble.SpyOrderIdGenerator;
import dev.hamm0000.autotrader.trading.order.application.service.testdouble.SpyOrderSubmitter;
import dev.hamm0000.autotrader.trading.order.domain.Order;
import dev.hamm0000.autotrader.trading.order.domain.OrderId;
import dev.hamm0000.autotrader.trading.order.domain.OrderIdempotencyKey;
import dev.hamm0000.autotrader.trading.order.domain.OrderState;
import dev.hamm0000.autotrader.trading.proposal.domain.BuyProposal;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.InstantSource;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderPlacementServiceTest {

    private static final OrderId ORDER_ID = new OrderId(
            UUID.fromString("63df7894-b456-4bf7-85cd-706d9d32c7c9")
    );
    private static final OrderIdempotencyKey IDEMPOTENCY_KEY = new OrderIdempotencyKey(
            "zscore:005930:2026-09-10"
    );
    private static final Instant NOW = Instant.parse("2026-09-10T00:05:00Z");
    private static final OrderAcknowledgment ACKNOWLEDGMENT =
            new OrderAcknowledgment("0000123456");

    @Test
    void 주문을_먼저_저장한_후_브로커에_제출하고_접수_상태를_저장한다() {
        // given
        FakeOrderStore orders = new FakeOrderStore();
        SpyOrderSubmitter submitter = new SpyOrderSubmitter(orders, ACKNOWLEDGMENT);
        SpyOrderIdGenerator ids = new SpyOrderIdGenerator(ORDER_ID);

        // when
        Order result = service(orders, submitter, ids).place(command());

        // then
        List<Order> savedOrders = orders.savedOrders();
        assertAll(
                () -> assertEquals(2, savedOrders.size()),
                () -> assertInstanceOf(OrderState.PendingSubmission.class, savedOrders.get(0).state()),
                () -> assertInstanceOf(OrderState.Submitted.class, savedOrders.get(1).state()),
                () -> assertEquals(savedOrders.get(0), submitter.submittedOrder()),
                () -> assertTrue(submitter.savedBeforeSubmission()),
                () -> assertEquals(ORDER_ID, result.id()),
                () -> assertEquals(IDEMPOTENCY_KEY, result.idempotencyKey()),
                () -> assertEquals(
                        new OrderState.Submitted(ACKNOWLEDGMENT.brokerOrderId(), NOW),
                        result.state()
                ),
                () -> assertEquals(1, ids.generateCount()),
                () -> assertEquals(1, submitter.submitCount())
        );
    }

    @Test
    void 같은_멱등키의_주문이_있으면_새_주문을_만들거나_제출하지_않는다() {
        // given
        FakeOrderStore orders = new FakeOrderStore();
        Order existing = Order.prepare(ORDER_ID, IDEMPOTENCY_KEY, command().proposal(), NOW);
        orders.put(existing);
        SpyOrderSubmitter submitter = new SpyOrderSubmitter(orders, ACKNOWLEDGMENT);
        SpyOrderIdGenerator ids = new SpyOrderIdGenerator(ORDER_ID);

        // when
        Order result = service(orders, submitter, ids).place(command());

        // then
        assertAll(
                () -> assertSame(existing, result),
                () -> assertEquals(0, ids.generateCount()),
                () -> assertEquals(0, submitter.submitCount()),
                () -> assertTrue(orders.savedOrders().isEmpty())
        );
    }

    @Test
    void 동시_요청이_먼저_저장되면_중복키_실패_후_기존_주문을_반환하고_제출하지_않는다() {
        // given
        FakeOrderStore orders = new FakeOrderStore();
        Order concurrent = Order.prepare(ORDER_ID, IDEMPOTENCY_KEY, command().proposal(), NOW);
        orders.failSaveAfterConcurrentOrder(
                new OrderSaveException("중복 멱등키", new RuntimeException("duplicate")),
                concurrent
        );
        SpyOrderSubmitter submitter = new SpyOrderSubmitter(orders, ACKNOWLEDGMENT);
        SpyOrderIdGenerator ids = new SpyOrderIdGenerator(ORDER_ID);

        // when
        Order result = service(orders, submitter, ids).place(command());

        // then
        assertAll(
                () -> assertSame(concurrent, result),
                () -> assertEquals(1, ids.generateCount()),
                () -> assertEquals(0, submitter.submitCount())
        );
    }

    @Test
    void 주문_최초_저장에_실패하면_브로커에_제출하지_않는다() {
        // given
        FakeOrderStore orders = new FakeOrderStore();
        OrderSaveException expected = new OrderSaveException(
                "저장 실패",
                new RuntimeException("database unavailable")
        );
        orders.failSaveWith(expected);
        SpyOrderSubmitter submitter = new SpyOrderSubmitter(orders, ACKNOWLEDGMENT);

        // when
        OrderSaveException actual = assertThrows(
                OrderSaveException.class,
                () -> service(orders, submitter, new SpyOrderIdGenerator(ORDER_ID))
                        .place(command())
        );

        // then
        assertAll(
                () -> assertSame(expected, actual),
                () -> assertEquals(0, submitter.submitCount())
        );
    }

    @Test
    void 주문_접수_후_상태_저장에_실패해도_같은_멱등키로_재제출하지_않는다() {
        // given
        FakeOrderStore orders = new FakeOrderStore();
        OrderSaveException expected = new OrderSaveException(
                "접수 상태 저장 실패",
                new RuntimeException("database unavailable")
        );
        orders.failOnSaveAttempt(2, expected);
        SpyOrderSubmitter submitter = new SpyOrderSubmitter(orders, ACKNOWLEDGMENT);
        OrderPlacementService service = service(
                orders,
                submitter,
                new SpyOrderIdGenerator(ORDER_ID)
        );

        // when
        OrderSaveException actual = assertThrows(
                OrderSaveException.class,
                () -> service.place(command())
        );
        Order retried = service.place(command());

        // then
        assertAll(
                () -> assertSame(expected, actual),
                () -> assertInstanceOf(OrderState.PendingSubmission.class, retried.state()),
                () -> assertEquals(1, orders.savedOrders().size()),
                () -> assertEquals(1, submitter.submitCount())
        );
    }

    @Test
    void 브로커가_주문을_거절하면_REJECTED_상태를_저장한다() {
        // given
        FakeOrderStore orders = new FakeOrderStore();
        SpyOrderSubmitter submitter = new SpyOrderSubmitter(orders, ACKNOWLEDGMENT);
        submitter.failWith(OrderSubmissionException.rejected("주문 가능 금액 부족"));

        // when
        Order result = service(orders, submitter, new SpyOrderIdGenerator(ORDER_ID))
                .place(command());

        // then
        assertEquals(
                new OrderState.Rejected("주문 가능 금액 부족", NOW),
                result.state()
        );
        assertEquals(result, orders.savedOrders().get(1));
    }

    @Test
    void 주문_요청_전_인증을_사용할_수_없으면_REJECTED_상태를_저장한다() {
        // given
        FakeOrderStore orders = new FakeOrderStore();
        SpyOrderSubmitter submitter = new SpyOrderSubmitter(orders, ACKNOWLEDGMENT);
        submitter.failWith(OrderSubmissionException.providerUnavailable(
                "주문 인증 실패",
                new RuntimeException("token unavailable")
        ));

        // when
        Order result = service(orders, submitter, new SpyOrderIdGenerator(ORDER_ID))
                .place(command());

        // then
        assertEquals(new OrderState.Rejected("주문 인증 실패", NOW), result.state());
        assertEquals(result, orders.savedOrders().get(1));
    }

    @Test
    void 주문_접수_결과를_확정할_수_없으면_SUBMISSION_UNKNOWN을_저장한다() {
        // given
        FakeOrderStore orders = new FakeOrderStore();
        SpyOrderSubmitter submitter = new SpyOrderSubmitter(orders, ACKNOWLEDGMENT);
        submitter.failWith(OrderSubmissionException.resultUnknown("응답 수신 전 연결 종료"));

        // when
        Order result = service(orders, submitter, new SpyOrderIdGenerator(ORDER_ID))
                .place(command());

        // then
        assertEquals(
                new OrderState.SubmissionUnknown("응답 수신 전 연결 종료", NOW),
                result.state()
        );
        assertEquals(result, orders.savedOrders().get(1));
    }

    private static OrderPlacementService service(
            FakeOrderStore orders,
            SpyOrderSubmitter submitter,
            SpyOrderIdGenerator ids
    ) {
        return new OrderPlacementService(
                orders,
                orders,
                submitter,
                ids,
                InstantSource.fixed(NOW)
        );
    }

    private static OrderPlacementCommand command() {
        return new OrderPlacementCommand(
                IDEMPOTENCY_KEY,
                new BuyProposal(
                        "005930",
                        138,
                        new BigDecimal("72300"),
                        new BigDecimal("10000000")
                )
        );
    }
}

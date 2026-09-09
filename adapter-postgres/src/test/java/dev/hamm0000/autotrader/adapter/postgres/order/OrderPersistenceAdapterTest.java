package dev.hamm0000.autotrader.adapter.postgres.order;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.hamm0000.autotrader.adapter.postgres.marketcalendar.PostgresAdapterTestApplication;
import dev.hamm0000.autotrader.trading.order.application.port.out.FindOrder;
import dev.hamm0000.autotrader.trading.order.application.port.out.OrderSaveException;
import dev.hamm0000.autotrader.trading.order.application.port.out.SaveOrder;
import dev.hamm0000.autotrader.trading.order.domain.Order;
import dev.hamm0000.autotrader.trading.order.domain.OrderId;
import dev.hamm0000.autotrader.trading.order.domain.OrderIdempotencyKey;
import dev.hamm0000.autotrader.trading.order.domain.OrderSide;
import dev.hamm0000.autotrader.trading.order.domain.OrderState;
import dev.hamm0000.autotrader.trading.order.domain.OrderType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(
        classes = PostgresAdapterTestApplication.class,
        properties = "spring.jpa.hibernate.ddl-auto=validate"
)
class OrderPersistenceAdapterTest {

    private static final OrderId ORDER_ID = new OrderId(
            UUID.fromString("63df7894-b456-4bf7-85cd-706d9d32c7c9")
    );
    private static final OrderIdempotencyKey IDEMPOTENCY_KEY = new OrderIdempotencyKey(
            "zscore:005930:2026-09-02"
    );
    private static final Instant PREPARED_AT = Instant.parse("2026-09-02T00:05:00Z");

    @Autowired
    private SaveOrder saveOrder;

    @Autowired
    private FindOrder findOrder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearTable() {
        jdbcTemplate.execute("TRUNCATE TABLE trade_order");
    }

    @Test
    void 준비된_시장가_주문을_저장하면_멱등키와_평가_기준가를_보존한다() {
        // given
        Order order = order(
                ORDER_ID,
                IDEMPOTENCY_KEY,
                new OrderState.PendingSubmission(PREPARED_AT)
        );

        // when
        saveOrder.save(order);

        // then
        Order found = findOrder.findByIdempotencyKey(IDEMPOTENCY_KEY).orElseThrow();
        assertAll(
                () -> assertEquals(order.id(), found.id()),
                () -> assertEquals(order.idempotencyKey(), found.idempotencyKey()),
                () -> assertEquals(order.symbol(), found.symbol()),
                () -> assertEquals(order.side(), found.side()),
                () -> assertEquals(order.quantity(), found.quantity()),
                () -> assertEquals(order.type(), found.type()),
                () -> assertEquals(order.preparedAt(), found.preparedAt()),
                () -> assertEquals(order.state(), found.state()),
                () -> assertEquals(
                        "PENDING_SUBMISSION",
                        jdbcTemplate.queryForObject(
                                "SELECT state FROM trade_order WHERE order_id = ?",
                                String.class,
                                ORDER_ID.value()
                        )
                ),
                () -> assertEquals(
                        0,
                        new BigDecimal("72300.0000").compareTo(found.referencePrice())
                )
        );
    }

    @ParameterizedTest
    @MethodSource("resolvedStates")
    void 제출_결과_상태를_저장하고_복원한다(OrderState state) {
        // given
        Order order = order(ORDER_ID, IDEMPOTENCY_KEY, state);

        // when
        saveOrder.save(order);

        // then
        assertEquals(
                state,
                findOrder.findByIdempotencyKey(IDEMPOTENCY_KEY).orElseThrow().state()
        );
    }

    @Test
    void 같은_주문의_상태를_다시_저장하면_새_행을_만들지_않고_갱신한다() {
        // given
        Order pending = order(
                ORDER_ID,
                IDEMPOTENCY_KEY,
                new OrderState.PendingSubmission(PREPARED_AT)
        );
        saveOrder.save(pending);

        // when
        saveOrder.save(pending.markSubmitted("0000123456", PREPARED_AT.plusSeconds(1)));

        // then
        Order found = findOrder.findByIdempotencyKey(IDEMPOTENCY_KEY).orElseThrow();
        assertAll(
                () -> assertInstanceOf(OrderState.Submitted.class, found.state()),
                () -> assertEquals(
                        1,
                        jdbcTemplate.queryForObject(
                                "SELECT count(*) FROM trade_order",
                                Integer.class
                        )
                )
        );
    }

    @Test
    void 같은_멱등키로_다른_주문을_저장하면_중복_주문을_차단한다() {
        // given
        saveOrder.save(order(
                ORDER_ID,
                IDEMPOTENCY_KEY,
                new OrderState.PendingSubmission(PREPARED_AT)
        ));
        Order duplicate = order(
                new OrderId(UUID.fromString("95c31c05-79b9-4c5a-89cb-68a7d6aa4bb1")),
                IDEMPOTENCY_KEY,
                new OrderState.PendingSubmission(PREPARED_AT)
        );

        // when & then
        assertThrows(OrderSaveException.class, () -> saveOrder.save(duplicate));
        assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM trade_order",
                        Integer.class
                )
        );
    }

    @Test
    void 저장되지_않은_멱등키는_조회되지_않는다() {
        assertTrue(findOrder.findByIdempotencyKey(IDEMPOTENCY_KEY).isEmpty());
    }

    private static Stream<Arguments> resolvedStates() {
        Instant occurredAt = PREPARED_AT.plusSeconds(1);
        return Stream.of(
                Arguments.of(new OrderState.Submitted("0000123456", occurredAt)),
                Arguments.of(new OrderState.Rejected("주문 가능 금액 부족", occurredAt)),
                Arguments.of(new OrderState.SubmissionUnknown("응답 수신 전 연결 종료", occurredAt))
        );
    }

    private static Order order(
            OrderId id,
            OrderIdempotencyKey idempotencyKey,
            OrderState state
    ) {
        return new Order(
                id,
                idempotencyKey,
                "005930",
                OrderSide.BUY,
                138,
                new BigDecimal("72300"),
                OrderType.MARKET,
                PREPARED_AT,
                state
        );
    }
}

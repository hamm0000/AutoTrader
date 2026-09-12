package dev.hamm0000.autotrader.trading.order.application.port.out;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class OrderAcknowledgmentTest {

    @Test
    void 접수_확인은_브로커_주문번호를_보존한다() {
        OrderAcknowledgment acknowledgment = new OrderAcknowledgment("0000123456");

        assertEquals("0000123456", acknowledgment.brokerOrderId());
    }

    @Test
    void 접수_확인의_브로커_주문번호는_공백일_수_없다() {
        assertThrows(IllegalArgumentException.class, () -> new OrderAcknowledgment("  "));
    }
}

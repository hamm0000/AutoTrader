package dev.hamm0000.autotrader.trading.order.application.port.out;

import java.util.Objects;

/**
 * 브로커가 주문 요청을 접수했다는 확인이며, 그때 발급된 주문번호를 담습니다.
 * <br/>
 * <b>주문의 체결을 의미하는 것은 아닙니다.</b>
 */
public record OrderAcknowledgment(String brokerOrderId) {

    public OrderAcknowledgment {
        Objects.requireNonNull(brokerOrderId, "brokerOrderId must not be null");
        if (brokerOrderId.isBlank()) {
            throw new IllegalArgumentException("brokerOrderId must not be blank");
        }
    }
}

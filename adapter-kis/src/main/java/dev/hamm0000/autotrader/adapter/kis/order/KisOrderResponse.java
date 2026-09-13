package dev.hamm0000.autotrader.adapter.kis.order;

import static dev.hamm0000.autotrader.trading.order.application.port.out.OrderSubmissionException.rejected;
import static dev.hamm0000.autotrader.trading.order.application.port.out.OrderSubmissionException.resultUnknown;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.hamm0000.autotrader.trading.order.application.port.out.OrderAcknowledgment;

@JsonIgnoreProperties(ignoreUnknown = true)
record KisOrderResponse(
        @JsonProperty("rt_cd") String returnCode,
        @JsonProperty("msg_cd") String messageCode,
        @JsonProperty("msg1") String message,
        KisOrderOutput output
) {

    OrderAcknowledgment toAcknowledgment() {
        if (returnCode == null || returnCode.isBlank()) {
            throw resultUnknown("KIS 주문 응답에 rt_cd가 없습니다");
        }
        if (!returnCode.equals("0")) {
            throw rejected(rejectionMessage());
        }
        if (output == null) {
            throw resultUnknown("KIS 주문 성공 응답에 output이 없습니다");
        }
        return output.toAcknowledgment();
    }

    private String rejectionMessage() {
        String code = messageCode == null || messageCode.isBlank() ? "UNKNOWN" : messageCode;
        String detail = message == null || message.isBlank() ? "사유 없음" : message;
        return "KIS가 주문을 거절했습니다: " + code + " - " + detail;
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
record KisOrderOutput(
        @JsonProperty("ODNO") String brokerOrderId
) {

    OrderAcknowledgment toAcknowledgment() {
        if (brokerOrderId == null || brokerOrderId.isBlank()) {
            throw resultUnknown("KIS 주문 성공 응답에 ODNO가 없습니다");
        }
        return new OrderAcknowledgment(brokerOrderId);
    }
}

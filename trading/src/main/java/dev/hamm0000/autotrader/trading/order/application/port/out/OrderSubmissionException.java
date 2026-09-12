package dev.hamm0000.autotrader.trading.order.application.port.out;

import java.util.Objects;

public final class OrderSubmissionException extends RuntimeException {

    private final Reason reason;

    private OrderSubmissionException(Reason reason, String message) {
        super(message);
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
    }

    private OrderSubmissionException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
    }

    public static OrderSubmissionException providerUnavailable(
            String message,
            Throwable cause
    ) {
        return new OrderSubmissionException(Reason.PROVIDER_UNAVAILABLE, message, cause);
    }

    public static OrderSubmissionException rejected(String message) {
        return new OrderSubmissionException(Reason.REJECTED, message);
    }

    public static OrderSubmissionException resultUnknown(String message) {
        return new OrderSubmissionException(Reason.RESULT_UNKNOWN, message);
    }

    public static OrderSubmissionException resultUnknown(String message, Throwable cause) {
        return new OrderSubmissionException(Reason.RESULT_UNKNOWN, message, cause);
    }

    public Reason reason() {
        return reason;
    }

    public enum Reason {
        /** 주문 요청 전에 인증을 사용할 수 없어 KIS에 제출하지 못함 */
        PROVIDER_UNAVAILABLE,
        /** KIS가 주문을 명시적으로 거절함 */
        REJECTED,
        /** 요청 전송 후 응답을 확정할 수 없어 접수 여부를 알 수 없음 */
        RESULT_UNKNOWN
    }
}

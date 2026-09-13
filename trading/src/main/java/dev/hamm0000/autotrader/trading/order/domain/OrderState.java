package dev.hamm0000.autotrader.trading.order.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * KIS 주문 제출 결과와 reconciliation에 따른 상태 전이를 관리하는 State Machine
 *
 * <pre>
 * PendingSubmission
 * - KIS 주문 접수 성공 -> Submitted
 * - 주문 요청 전 제출 불가 또는 KIS의 명시적 주문 거절 -> Rejected
 * - 주문 응답 결과 확인 불가(timeout등) -> SubmissionUnknown
 *
 *  SubmissionUnknown(구현 예정)
 *   - 바로 재주문하지 않고 reconciliation 전까지 SubmissionUnknown 유지
 *   - reconciliation 후 주문 접수 확인 -> Submitted
 *   - reconciliation 후 주문 미접수 확인 -> Rejected
 * </pre>
 */
public sealed interface OrderState
        permits OrderState.PendingSubmission,
                OrderState.Submitted,
                OrderState.Rejected,
                OrderState.SubmissionUnknown {

    Instant occurredAt();

    record PendingSubmission(Instant occurredAt) implements OrderState {

        public PendingSubmission {
            Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        }
    }

    record Submitted(String brokerOrderId, Instant occurredAt) implements OrderState {

        public Submitted {
            requireNotBlank(brokerOrderId, "brokerOrderId");
            Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        }
    }

    record Rejected(String reason, Instant occurredAt) implements OrderState {

        public Rejected {
            requireNotBlank(reason, "reason");
            Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        }
    }

    record SubmissionUnknown(String reason, Instant occurredAt) implements OrderState {

        public SubmissionUnknown {
            requireNotBlank(reason, "reason");
            Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        }
    }

    private static String requireNotBlank(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}

package dev.hamm0000.autotrader.adapter.kis.http;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;

/**
 * KIS 조회 요청이 호출 제한으로 거절된 경우에만 한 번 더 시도합니다.
 * <br/>
 * 조회는 같은 요청을 다시 보내도 외부 상태를 바꾸지 않지만,
 * 주문은 결과가 불명확한 상태에서 다시 보내면 중복 주문이 될 수 있어 이 클래스를 사용하지 않습니다.
 */
public final class KisReadRequestExecutor {

    private static final String RATE_LIMIT_MESSAGE_CODE = "EGW00201";
    private static final System.Logger log =
            System.getLogger(KisReadRequestExecutor.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private KisReadRequestExecutor() {
    }

    public static <T> T execute(Supplier<T> request) {
        Objects.requireNonNull(request, "request must not be null");

        try {
            return request.get();
        } catch (RestClientException firstFailure) {
            if (!isRateLimitFailure(firstFailure)) {
                throw firstFailure;
            }

            log.log(
                    System.Logger.Level.WARNING,
                    "KIS 호출 제한 응답으로 조회 요청을 한 번 재시도합니다 (msgCd={0})",
                    RATE_LIMIT_MESSAGE_CODE
            );
            return request.get();
        }
    }

    private static boolean isRateLimitFailure(RestClientException failure) {
        RestClientResponseException responseFailure = findResponseFailure(failure);
        if (responseFailure == null || responseFailure.getStatusCode().value() != 500) {
            return false;
        }

        try {
            KisErrorResponse response = OBJECT_MAPPER.readValue(
                    responseFailure.getResponseBodyAsByteArray(),
                    KisErrorResponse.class
            );
            return RATE_LIMIT_MESSAGE_CODE.equals(response.messageCode());
        } catch (Exception ignored) {
            // 오류 응답을 해석하지 못하면 호출 제한으로 추정하지 않고 원래 실패를 그대로 전달
            return false;
        }
    }

    private static RestClientResponseException findResponseFailure(Throwable failure) {
        Throwable cause = failure;
        while (cause != null) {
            if (cause instanceof RestClientResponseException responseFailure) {
                return responseFailure;
            }
            cause = cause.getCause();
        }
        return null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KisErrorResponse(@JsonProperty("msg_cd") String messageCode) {
    }
}

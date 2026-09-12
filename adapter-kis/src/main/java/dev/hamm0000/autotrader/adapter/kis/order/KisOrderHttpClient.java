package dev.hamm0000.autotrader.adapter.kis.order;

import static dev.hamm0000.autotrader.trading.order.application.port.out.OrderSubmissionException.providerUnavailable;
import static dev.hamm0000.autotrader.trading.order.application.port.out.OrderSubmissionException.resultUnknown;

import dev.hamm0000.autotrader.adapter.kis.auth.AccessToken;
import dev.hamm0000.autotrader.adapter.kis.auth.KisAccessTokenProvider;
import dev.hamm0000.autotrader.adapter.kis.auth.KisAuthException;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

final class KisOrderHttpClient {

    private final RestClient restClient;
    private final KisAccessTokenProvider tokenProvider;
    private final String appKey;
    private final String appSecret;

    KisOrderHttpClient(
            RestClient restClient,
            KisAccessTokenProvider tokenProvider,
            String appKey,
            String appSecret
    ) {
        this.restClient = Objects.requireNonNull(restClient, "restClient must not be null");
        this.tokenProvider = Objects.requireNonNull(tokenProvider, "tokenProvider must not be null");
        this.appKey = requireNotBlank(appKey, "appKey");
        this.appSecret = requireNotBlank(appSecret, "appSecret");
    }

    KisOrderResponse post(String path, String transactionId, KisOrderRequest request) {
        AccessToken token;
        try {
            token = tokenProvider.getAccessToken();
        } catch (KisAuthException exception) {
            throw providerUnavailable(
                    "KIS 주문 제출에 필요한 인증을 사용할 수 없습니다",
                    exception
            );
        }

        try {
            KisOrderResponse response = restClient.post()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.value())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("appkey", appKey)
                    .header("appsecret", appSecret)
                    .header("tr_id", transactionId)
                    .header("custtype", "P")
                    .body(request)
                    .retrieve()
                    .body(KisOrderResponse.class);
            if (response == null) {
                throw resultUnknown("KIS 주문 응답이 비어 있습니다");
            }
            return response;
        } catch (RestClientException exception) {
            throw resultUnknown("KIS 주문 제출 결과를 확인할 수 없습니다", exception);
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

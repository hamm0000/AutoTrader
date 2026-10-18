package dev.hamm0000.autotrader.adapter.kis.query.calendar;

import static dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.status.TradingDayStatusQueryException.invalidResponse;
import static dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.status.TradingDayStatusQueryException.providerUnavailable;

import dev.hamm0000.autotrader.adapter.kis.auth.AccessToken;
import dev.hamm0000.autotrader.adapter.kis.auth.KisAccessTokenProvider;
import dev.hamm0000.autotrader.adapter.kis.auth.KisAuthException;
import dev.hamm0000.autotrader.adapter.kis.http.KisReadRequestExecutor;
import java.net.URI;
import java.util.Objects;
import java.util.function.Function;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

final class KisCalendarQueryHttpClient {

    private final RestClient restClient;
    private final KisAccessTokenProvider tokenProvider;
    private final String appKey;
    private final String appSecret;

    KisCalendarQueryHttpClient(
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

    <T> T get(
            Function<UriBuilder, URI> uriFunction,
            String transactionId,
            Class<T> responseType,
            String dataName
    ) {
        try {
            AccessToken token = tokenProvider.getAccessToken();
            ResponseEntity<T> response = KisReadRequestExecutor.execute(() -> restClient.get()
                    .uri(uriFunction)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.value())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("appkey", appKey)
                    .header("appsecret", appSecret)
                    .header("tr_id", transactionId)
                    .header("custtype", "P")
                    .retrieve()
                    .toEntity(responseType));
            if (response.getBody() == null) {
                throw invalidResponse("KIS " + dataName + " 응답이 비어 있습니다");
            }
            return response.getBody();
        } catch (KisAuthException exception) {
            throw providerUnavailable(
                    "KIS " + dataName + " 조회에 필요한 인증을 사용할 수 없습니다",
                    exception
            );
        } catch (RestClientException exception) {
            if (hasCause(exception, HttpMessageConversionException.class)) {
                throw invalidResponse("KIS " + dataName + " 응답을 해석할 수 없습니다", exception);
            }
            throw providerUnavailable("KIS " + dataName + " 조회 요청이 실패했습니다", exception);
        }
    }

    private static String requireNotBlank(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static boolean hasCause(Throwable failure, Class<? extends Throwable> causeType) {
        Throwable cause = failure;
        while (cause != null) {
            if (causeType.isInstance(cause)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}

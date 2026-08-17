package dev.hamm0000.autotrader.adapter.kis.query;

import static dev.hamm0000.autotrader.trading.marketdata.application.port.out.MarketDataQueryException.invalidResponse;
import static dev.hamm0000.autotrader.trading.marketdata.application.port.out.MarketDataQueryException.providerUnavailable;

import dev.hamm0000.autotrader.adapter.kis.auth.AccessToken;
import dev.hamm0000.autotrader.adapter.kis.auth.KisAccessTokenProvider;
import dev.hamm0000.autotrader.adapter.kis.auth.KisAuthException;
import dev.hamm0000.autotrader.trading.marketdata.application.port.out.LoadRecentClosingPrices;
import dev.hamm0000.autotrader.trading.marketdata.application.port.out.RecentClosingPricesQuery;
import dev.hamm0000.autotrader.trading.marketdata.domain.RecentClosingPrices;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public final class KisDailyClosingPriceApi implements LoadRecentClosingPrices {

    private static final String DAILY_CLOSING_PRICE_PATH =
            "/uapi/domestic-stock/v1/quotations/inquire-daily-price";
    private static final String TRANSACTION_ID = "FHKST01010400";

    private final RestClient restClient;
    private final KisAccessTokenProvider tokenProvider;
    private final String appKey;
    private final String appSecret;

    public KisDailyClosingPriceApi(
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

    @Override
    public RecentClosingPrices load(RecentClosingPricesQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        KisDailyClosingPriceResponse response = requestDailyClosingPrices(query);
        if (response == null) {
            throw invalidResponse("KIS 최근 종가 응답이 비어 있습니다");
        }
        return response.toRecentClosingPrices(query);
    }

    private KisDailyClosingPriceResponse requestDailyClosingPrices(RecentClosingPricesQuery query) {
        try {
            AccessToken token = tokenProvider.getAccessToken();
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(DAILY_CLOSING_PRICE_PATH)
                            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                            .queryParam("FID_INPUT_ISCD", query.symbol())
                            .queryParam("FID_PERIOD_DIV_CODE", "D")
                            .queryParam("FID_ORG_ADJ_PRC", "1")
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.value())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("appkey", appKey)
                    .header("appsecret", appSecret)
                    .header("tr_id", TRANSACTION_ID)
                    .header("custtype", "P")
                    .retrieve()
                    .body(KisDailyClosingPriceResponse.class);
        } catch (KisAuthException exception) {
            throw providerUnavailable("KIS 최근 종가 조회에 필요한 인증을 사용할 수 없습니다", exception);
        } catch (RestClientException exception) {
            if (hasCause(exception, HttpMessageConversionException.class)) {
                throw invalidResponse("KIS 최근 종가 응답을 해석할 수 없습니다", exception);
            }
            throw providerUnavailable("KIS 최근 종가 조회 요청이 실패했습니다", exception);
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

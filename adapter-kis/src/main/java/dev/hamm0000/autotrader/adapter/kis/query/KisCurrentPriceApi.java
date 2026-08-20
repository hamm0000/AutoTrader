package dev.hamm0000.autotrader.adapter.kis.query;

import dev.hamm0000.autotrader.adapter.kis.auth.KisAccessTokenProvider;
import dev.hamm0000.autotrader.trading.marketdata.application.port.out.CurrentPriceQuery;
import dev.hamm0000.autotrader.trading.marketdata.application.port.out.LoadCurrentPrice;
import dev.hamm0000.autotrader.trading.marketdata.domain.CurrentPrice;
import java.util.Objects;
import org.springframework.web.client.RestClient;

public final class KisCurrentPriceApi implements LoadCurrentPrice {

    private static final String CURRENT_PRICE_PATH =
            "/uapi/domestic-stock/v1/quotations/inquire-price";
    private static final String TRANSACTION_ID = "FHKST01010100";

    private final KisQueryHttpClient httpClient;

    public KisCurrentPriceApi(
            RestClient restClient,
            KisAccessTokenProvider tokenProvider,
            String appKey,
            String appSecret
    ) {
        this.httpClient = new KisQueryHttpClient(restClient, tokenProvider, appKey, appSecret);
    }

    @Override
    public CurrentPrice load(CurrentPriceQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        KisCurrentPriceResponse response = requestCurrentPrice(query);
        return response.toCurrentPrice();
    }

    private KisCurrentPriceResponse requestCurrentPrice(CurrentPriceQuery query) {
        return httpClient.get(
                uriBuilder -> uriBuilder
                        .path(CURRENT_PRICE_PATH)
                        .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                        .queryParam("FID_INPUT_ISCD", query.symbol())
                        .build(),
                TRANSACTION_ID,
                KisCurrentPriceResponse.class,
                "현재가"
        );
    }
}

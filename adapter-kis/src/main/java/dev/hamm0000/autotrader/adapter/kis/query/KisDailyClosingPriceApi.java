package dev.hamm0000.autotrader.adapter.kis.query;

import dev.hamm0000.autotrader.adapter.kis.auth.KisAccessTokenProvider;
import dev.hamm0000.autotrader.trading.marketdata.application.port.out.LoadRecentClosingPrices;
import dev.hamm0000.autotrader.trading.marketdata.application.port.out.RecentClosingPricesQuery;
import dev.hamm0000.autotrader.trading.marketdata.domain.RecentClosingPrices;
import java.util.Objects;
import org.springframework.web.client.RestClient;

public final class KisDailyClosingPriceApi implements LoadRecentClosingPrices {

    private static final String DAILY_CLOSING_PRICE_PATH =
            "/uapi/domestic-stock/v1/quotations/inquire-daily-price";
    private static final String TRANSACTION_ID = "FHKST01010400";

    private final KisQueryHttpClient httpClient;

    public KisDailyClosingPriceApi(
            RestClient restClient,
            KisAccessTokenProvider tokenProvider,
            String appKey,
            String appSecret
    ) {
        this.httpClient = new KisQueryHttpClient(restClient, tokenProvider, appKey, appSecret);
    }

    @Override
    public RecentClosingPrices load(RecentClosingPricesQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        KisDailyClosingPriceResponse response = requestDailyClosingPrices(query);
        return response.toRecentClosingPrices(query);
    }

    private KisDailyClosingPriceResponse requestDailyClosingPrices(RecentClosingPricesQuery query) {
        return httpClient.get(
                uriBuilder -> uriBuilder
                        .path(DAILY_CLOSING_PRICE_PATH)
                        .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                        .queryParam("FID_INPUT_ISCD", query.symbol())
                        .queryParam("FID_PERIOD_DIV_CODE", "D")
                        .queryParam("FID_ORG_ADJ_PRC", "1")
                        .build(),
                TRANSACTION_ID,
                KisDailyClosingPriceResponse.class,
                "최근 종가"
        );
    }
}

package dev.hamm0000.autotrader.adapter.kis.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import dev.hamm0000.autotrader.adapter.kis.auth.AccessToken;
import dev.hamm0000.autotrader.adapter.kis.auth.KisAccessTokenProvider;
import dev.hamm0000.autotrader.adapter.kis.auth.KisAuthException;
import dev.hamm0000.autotrader.trading.marketdata.application.port.out.MarketDataQueryException;
import dev.hamm0000.autotrader.trading.marketdata.application.port.out.RecentClosingPricesQuery;
import dev.hamm0000.autotrader.trading.marketdata.domain.DailyClosingPrice;
import dev.hamm0000.autotrader.trading.marketdata.domain.RecentClosingPrices;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.InstantSource;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KisDailyClosingPriceApiTest {

    private static final String BASE_URL = "http://kis.test";
    private static final String DAILY_CLOSING_PRICE_PATH =
            "/uapi/domestic-stock/v1/quotations/inquire-daily-price";
    private static final String APP_KEY = "app-key-value";
    private static final String APP_SECRET = "app-secret-value";
    private static final Instant NOW = Instant.parse("2026-08-17T00:00:00Z");

    @Test
    void 요청한_개수보다_완료된_거래일이_많으면_최근순으로_골라_오래된_순서로_반환한다() {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(startsWith(BASE_URL + DAILY_CLOSING_PRICE_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer issued-token-value"))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header("appkey", APP_KEY))
                .andExpect(header("appsecret", APP_SECRET))
                .andExpect(header("tr_id", "FHKST01010400"))
                .andExpect(header("custtype", "P"))
                .andExpect(queryParam("FID_COND_MRKT_DIV_CODE", "J"))
                .andExpect(queryParam("FID_INPUT_ISCD", "005930"))
                .andExpect(queryParam("FID_PERIOD_DIV_CODE", "D"))
                .andExpect(queryParam("FID_ORG_ADJ_PRC", "1"))
                .andRespond(withSuccess(successfulClosingPriceResponse(), MediaType.APPLICATION_JSON));
        KisDailyClosingPriceApi api = api(builder);

        // when
        RecentClosingPrices result = api.load(query(2));

        // then
        assertThat(result.values())
                .extracting(DailyClosingPrice::tradingDate)
                .containsExactly(LocalDate.parse("2026-08-13"), LocalDate.parse("2026-08-14"));
        assertThat(result.values())
                .extracting(DailyClosingPrice::price)
                .containsExactly(new BigDecimal("70000"), new BigDecimal("71000"));
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
            {"rt_cd":"0"}""",
            """
            {"rt_cd":"0","output":[{"stck_clpr":"71000"}]}""",
            """
            {"rt_cd":"0","output":[{"stck_bsop_date":"20260814"}]}""",
            """
            {"output":[{"stck_bsop_date":"20260814","stck_clpr":"71000"}]}""",
            """
            {"rt_cd":"0","output":[null]}""",
            """
            {"rt_cd":"0","output":[{"stck_bsop_date":"invalid","stck_clpr":"71000"}]}""",
            """
            {"rt_cd":"0","output":[{"stck_bsop_date":"20260814","stck_clpr":"invalid"}]}"""
    })
    void 최근_종가_응답이_올바르지_않으면_INVALID_RESPONSE로_변환한다(String response) {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(startsWith(BASE_URL + DAILY_CLOSING_PRICE_PATH)))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        KisDailyClosingPriceApi api = api(builder);

        // when & then
        assertThatThrownBy(() -> api.load(query(1)))
                .isInstanceOfSatisfying(MarketDataQueryException.class, exception ->
                        assertThat(exception.reason())
                                .isEqualTo(MarketDataQueryException.Reason.INVALID_RESPONSE));
        server.verify();
    }

    @Test
    void 최근_종가_응답이_비어있으면_INVALID_RESPONSE로_변환한다() {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(startsWith(BASE_URL + DAILY_CLOSING_PRICE_PATH)))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));
        KisDailyClosingPriceApi api = api(builder);

        // when & then
        assertThatThrownBy(() -> api.load(query(1)))
                .isInstanceOfSatisfying(MarketDataQueryException.class, exception ->
                        assertThat(exception.reason())
                                .isEqualTo(MarketDataQueryException.Reason.INVALID_RESPONSE));
        server.verify();
    }

    @Test
    void 완료된_종가가_요청한_개수보다_적으면_INSUFFICIENT_DATA로_변환한다() {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(startsWith(BASE_URL + DAILY_CLOSING_PRICE_PATH)))
                .andRespond(withSuccess("""
                        {
                          "rt_cd": "0",
                          "output": [
                            {"stck_bsop_date": "20260817", "stck_clpr": "72000"},
                            {"stck_bsop_date": "20260814", "stck_clpr": "71000"}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));
        KisDailyClosingPriceApi api = api(builder);

        // when & then
        assertThatThrownBy(() -> api.load(query(2)))
                .isInstanceOfSatisfying(MarketDataQueryException.class, exception ->
                        assertThat(exception.reason())
                                .isEqualTo(MarketDataQueryException.Reason.INSUFFICIENT_DATA));
        server.verify();
    }

    @Test
    void KIS가_오류_상태코드를_반환하면_PROVIDER_UNAVAILABLE로_변환한다() {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(startsWith(BASE_URL + DAILY_CLOSING_PRICE_PATH)))
                .andRespond(withServerError());
        KisDailyClosingPriceApi api = api(builder);

        // when & then
        assertThatThrownBy(() -> api.load(query(1)))
                .isInstanceOfSatisfying(MarketDataQueryException.class, exception ->
                        assertThat(exception.reason())
                                .isEqualTo(MarketDataQueryException.Reason.PROVIDER_UNAVAILABLE));
        server.verify();
    }

    @Test
    void KIS가_실패_응답을_반환하면_PROVIDER_UNAVAILABLE로_변환한다() {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(startsWith(BASE_URL + DAILY_CLOSING_PRICE_PATH)))
                .andRespond(withSuccess("""
                        {"rt_cd":"1", "msg_cd":"ERROR", "msg1":"요청 실패"}
                        """, MediaType.APPLICATION_JSON));
        KisDailyClosingPriceApi api = api(builder);

        // when & then
        assertThatThrownBy(() -> api.load(query(1)))
                .isInstanceOfSatisfying(MarketDataQueryException.class, exception ->
                        assertThat(exception.reason())
                                .isEqualTo(MarketDataQueryException.Reason.PROVIDER_UNAVAILABLE));
        server.verify();
    }

    @Test
    void 토큰_발급에_실패하면_PROVIDER_UNAVAILABLE로_변환한다() {
        // given
        KisDailyClosingPriceApi api = apiWithFailingAuth();

        // when & then
        assertThatThrownBy(() -> api.load(query(1)))
                .isInstanceOfSatisfying(MarketDataQueryException.class, exception ->
                        assertThat(exception.reason())
                                .isEqualTo(MarketDataQueryException.Reason.PROVIDER_UNAVAILABLE));
    }

    private static KisDailyClosingPriceApi api(RestClient.Builder builder) {
        KisAccessTokenProvider tokenProvider = new KisAccessTokenProvider(
                () -> new AccessToken("issued-token-value", NOW.plusSeconds(3600)),
                InstantSource.fixed(NOW)
        );
        return new KisDailyClosingPriceApi(builder.build(), tokenProvider, APP_KEY, APP_SECRET);
    }

    private static KisDailyClosingPriceApi apiWithFailingAuth() {
        KisAccessTokenProvider tokenProvider = new KisAccessTokenProvider(
                () -> { throw new KisAuthException("발급 실패"); },
                InstantSource.fixed(NOW)
        );
        return new KisDailyClosingPriceApi(
                RestClient.builder().baseUrl(BASE_URL).build(),
                tokenProvider, APP_KEY, APP_SECRET);
    }

    private static RecentClosingPricesQuery query(int count) {
        return new RecentClosingPricesQuery(
                "005930",
                count,
                LocalDate.parse("2026-08-17")
        );
    }

    private static String successfulClosingPriceResponse() {
        return """
                {
                  "rt_cd": "0",
                  "output": [
                    {"stck_bsop_date": "20260817", "stck_clpr": "72000"},
                    {"stck_bsop_date": "20260812", "stck_clpr": "69000"},
                    {"stck_bsop_date": "20260814", "stck_clpr": "71000"},
                    {"stck_bsop_date": "20260813", "stck_clpr": "70000"}
                  ]
                }
                """;
    }
}

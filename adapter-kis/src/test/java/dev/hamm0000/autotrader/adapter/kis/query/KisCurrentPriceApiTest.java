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
import dev.hamm0000.autotrader.trading.marketdata.application.port.out.CurrentPriceQuery;
import dev.hamm0000.autotrader.trading.marketdata.application.port.out.MarketDataQueryException;
import dev.hamm0000.autotrader.trading.marketdata.domain.CurrentPrice;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.InstantSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KisCurrentPriceApiTest {

    private static final String BASE_URL = "http://kis.test";
    private static final String CURRENT_PRICE_PATH =
            "/uapi/domestic-stock/v1/quotations/inquire-price";
    private static final String APP_KEY = "app-key-value";
    private static final String APP_SECRET = "app-secret-value";
    private static final Instant NOW = Instant.parse("2026-08-17T00:00:00Z");

    @Test
    void 현재가를_요청하면_응답으로_현재가를_반환한다() {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(startsWith(BASE_URL + CURRENT_PRICE_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer issued-token-value"))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header("appkey", APP_KEY))
                .andExpect(header("appsecret", APP_SECRET))
                .andExpect(header("tr_id", "FHKST01010100"))
                .andExpect(header("custtype", "P"))
                .andExpect(queryParam("FID_COND_MRKT_DIV_CODE", "J"))
                .andExpect(queryParam("FID_INPUT_ISCD", "005930"))
                .andRespond(withSuccess("""
                        {
                          "rt_cd": "0",
                          "output": {"stck_prpr": "70123.45"}
                        }
                        """, MediaType.APPLICATION_JSON));
        KisCurrentPriceApi api = api(builder);

        // when
        CurrentPrice result = api.load(new CurrentPriceQuery("005930"));

        // then
        assertThat(result.value()).isEqualTo(new BigDecimal("70123.45"));
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
            {"rt_cd":"0"}""",
            """
            {"rt_cd":"0","output":{}}""",
            """
            {"output":{"stck_prpr":"70123.45"}}""",
            """
            {"rt_cd":"0","output":{"stck_prpr":"invalid"}}"""
    })
    void 현재가_응답이_올바르지_않으면_INVALID_RESPONSE로_변환한다(String response) {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(startsWith(BASE_URL + CURRENT_PRICE_PATH)))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        KisCurrentPriceApi api = api(builder);

        // when & then
        assertThatThrownBy(() -> api.load(new CurrentPriceQuery("005930")))
                .isInstanceOfSatisfying(MarketDataQueryException.class, exception ->
                        assertThat(exception.reason())
                                .isEqualTo(MarketDataQueryException.Reason.INVALID_RESPONSE));
        server.verify();
    }

    @Test
    void 현재가_응답이_비어있으면_INVALID_RESPONSE로_변환한다() {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(startsWith(BASE_URL + CURRENT_PRICE_PATH)))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));
        KisCurrentPriceApi api = api(builder);

        // when & then
        assertThatThrownBy(() -> api.load(new CurrentPriceQuery("005930")))
                .isInstanceOfSatisfying(MarketDataQueryException.class, exception ->
                        assertThat(exception.reason())
                                .isEqualTo(MarketDataQueryException.Reason.INVALID_RESPONSE));
        server.verify();
    }

    @Test
    void KIS가_오류_상태코드를_반환하면_PROVIDER_UNAVAILABLE로_변환한다() {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(startsWith(BASE_URL + CURRENT_PRICE_PATH)))
                .andRespond(withServerError());
        KisCurrentPriceApi api = api(builder);

        // when & then
        assertThatThrownBy(() -> api.load(new CurrentPriceQuery("005930")))
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
        server.expect(requestTo(startsWith(BASE_URL + CURRENT_PRICE_PATH)))
                .andRespond(withSuccess("""
                        {"rt_cd":"1", "msg_cd":"ERROR", "msg1":"요청 실패"}
                        """, MediaType.APPLICATION_JSON));
        KisCurrentPriceApi api = api(builder);

        // when & then
        assertThatThrownBy(() -> api.load(new CurrentPriceQuery("005930")))
                .isInstanceOfSatisfying(MarketDataQueryException.class, exception ->
                        assertThat(exception.reason())
                                .isEqualTo(MarketDataQueryException.Reason.PROVIDER_UNAVAILABLE));
        server.verify();
    }

    @Test
    void 토큰_발급에_실패하면_PROVIDER_UNAVAILABLE로_변환한다() {
        // given
        KisCurrentPriceApi api = apiWithFailingAuth();

        // when & then
        assertThatThrownBy(() -> api.load(new CurrentPriceQuery("005930")))
                .isInstanceOfSatisfying(MarketDataQueryException.class, exception ->
                        assertThat(exception.reason())
                                .isEqualTo(MarketDataQueryException.Reason.PROVIDER_UNAVAILABLE));
    }

    private static KisCurrentPriceApi api(RestClient.Builder builder) {
        KisAccessTokenProvider tokenProvider = new KisAccessTokenProvider(
                () -> new AccessToken("issued-token-value", NOW.plusSeconds(3600)),
                InstantSource.fixed(NOW)
        );
        return new KisCurrentPriceApi(builder.build(), tokenProvider, APP_KEY, APP_SECRET);
    }

    private static KisCurrentPriceApi apiWithFailingAuth() {
        KisAccessTokenProvider tokenProvider = new KisAccessTokenProvider(
                () -> { throw new KisAuthException("발급 실패"); },
                InstantSource.fixed(NOW)
        );
        return new KisCurrentPriceApi(
                RestClient.builder().baseUrl(BASE_URL).build(),
                tokenProvider, APP_KEY, APP_SECRET);
    }
}

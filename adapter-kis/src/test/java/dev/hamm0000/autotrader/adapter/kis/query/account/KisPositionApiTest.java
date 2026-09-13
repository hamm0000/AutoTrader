package dev.hamm0000.autotrader.adapter.kis.query.account;

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
import dev.hamm0000.autotrader.trading.position.application.port.out.PositionQuery;
import dev.hamm0000.autotrader.trading.position.application.port.out.PositionQueryException;
import dev.hamm0000.autotrader.trading.position.domain.Position;
import java.time.Instant;
import java.time.InstantSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KisPositionApiTest {

    private static final String BASE_URL = "http://kis.test";
    private static final String BALANCE_PATH =
            "/uapi/domestic-stock/v1/trading/inquire-balance";
    private static final String APP_KEY = "app-key-value";
    private static final String APP_SECRET = "app-secret-value";
    private static final String ACCOUNT_NUMBER = "account-number-value";
    private static final String ACCOUNT_PRODUCT_CODE = "01";
    private static final Instant NOW = Instant.parse("2026-09-02T00:00:00Z");

    @Test
    void 모의계좌_잔고를_조회하면_요청한_종목의_보유수량을_반환한다() {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(startsWith(BASE_URL + BALANCE_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer issued-token-value"))
                .andExpect(header("appkey", APP_KEY))
                .andExpect(header("appsecret", APP_SECRET))
                .andExpect(header("tr_id", "VTTC8434R"))
                .andExpect(header("custtype", "P"))
                .andExpect(queryParam("CANO", ACCOUNT_NUMBER))
                .andExpect(queryParam("ACNT_PRDT_CD", ACCOUNT_PRODUCT_CODE))
                .andExpect(queryParam("AFHR_FLPR_YN", "N"))
                .andExpect(queryParam("INQR_DVSN", "02"))
                .andExpect(queryParam("UNPR_DVSN", "01"))
                .andExpect(queryParam("FUND_STTL_ICLD_YN", "N"))
                .andExpect(queryParam("FNCG_AMT_AUTO_RDPT_YN", "N"))
                .andExpect(queryParam("PRCS_DVSN", "00"))
                .andRespond(withSuccess("""
                        {
                          "rt_cd": "0",
                          "output1": [
                            {"pdno": "105560", "hldg_qty": "3"},
                            {"pdno": "005930", "hldg_qty": "7"}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));
        KisPositionApi api = api(builder);

        // when
        Position result = api.load(new PositionQuery("005930"));

        // then
        assertThat(result).isEqualTo(new Position("005930", 7));
        server.verify();
    }

    @Test
    void 모의계좌에_종목이_없으면_보유수량_0을_반환한다() {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(startsWith(BASE_URL + BALANCE_PATH)))
                .andRespond(withSuccess(successResponse("[]"), MediaType.APPLICATION_JSON));

        // when
        Position result = api(builder).load(new PositionQuery("005930"));

        // then
        assertThat(result).isEqualTo(new Position("005930", 0));
        server.verify();
    }

    @Test
    void 잔고가_단일_응답_한도를_초과하면_보유수량_0으로_판단하지_않는다() {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(startsWith(BASE_URL + BALANCE_PATH)))
                .andRespond(withSuccess(successResponse("[]"), MediaType.APPLICATION_JSON)
                        .header("tr_cont", "M"));

        // when & then
        assertThatThrownBy(() -> api(builder).load(new PositionQuery("005930")))
                .isInstanceOfSatisfying(PositionQueryException.class, exception ->
                        assertThat(exception.reason())
                                .isEqualTo(PositionQueryException.Reason.INVALID_RESPONSE));
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
            {"output1":[]}""",
            """
            {"rt_cd":"0"}""",
            """
            {"rt_cd":"0","output1":[{"hldg_qty":"7"}]}""",
            """
            {"rt_cd":"0","output1":[{"pdno":"005930","hldg_qty":"invalid"}]}""",
            """
            {"rt_cd":"0","output1":[{"pdno":"005930","hldg_qty":"-1"}]}"""
    })
    void 잔고_응답이_올바르지_않으면_INVALID_RESPONSE로_변환한다(String response) {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(startsWith(BASE_URL + BALANCE_PATH)))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        // when & then
        assertThatThrownBy(() -> api(builder).load(new PositionQuery("005930")))
                .isInstanceOfSatisfying(PositionQueryException.class, exception ->
                        assertThat(exception.reason())
                                .isEqualTo(PositionQueryException.Reason.INVALID_RESPONSE));
        server.verify();
    }

    @Test
    void KIS_잔고조회가_실패하면_PROVIDER_UNAVAILABLE로_변환한다() {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(startsWith(BASE_URL + BALANCE_PATH)))
                .andRespond(withServerError());

        // when & then
        assertThatThrownBy(() -> api(builder).load(new PositionQuery("005930")))
                .isInstanceOfSatisfying(PositionQueryException.class, exception ->
                        assertThat(exception.reason())
                                .isEqualTo(PositionQueryException.Reason.PROVIDER_UNAVAILABLE));
        server.verify();
    }

    @Test
    void KIS가_잔고_실패응답을_반환하면_PROVIDER_UNAVAILABLE로_변환한다() {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(startsWith(BASE_URL + BALANCE_PATH)))
                .andRespond(withSuccess("""
                        {"rt_cd":"1","msg_cd":"ERROR","msg1":"요청 실패"}
                        """, MediaType.APPLICATION_JSON));

        // when & then
        assertThatThrownBy(() -> api(builder).load(new PositionQuery("005930")))
                .isInstanceOfSatisfying(PositionQueryException.class, exception ->
                        assertThat(exception.reason())
                                .isEqualTo(PositionQueryException.Reason.PROVIDER_UNAVAILABLE));
        server.verify();
    }

    @Test
    void 토큰_발급에_실패하면_잔고를_PROVIDER_UNAVAILABLE로_변환한다() {
        // given
        KisAccessTokenProvider tokenProvider = new KisAccessTokenProvider(
                () -> { throw new KisAuthException("발급 실패"); },
                InstantSource.fixed(NOW)
        );
        KisPositionApi api = new KisPositionApi(
                RestClient.builder().baseUrl(BASE_URL).build(),
                tokenProvider,
                APP_KEY,
                APP_SECRET,
                ACCOUNT_NUMBER,
                ACCOUNT_PRODUCT_CODE
        );

        // when & then
        assertThatThrownBy(() -> api.load(new PositionQuery("005930")))
                .isInstanceOfSatisfying(PositionQueryException.class, exception ->
                        assertThat(exception.reason())
                                .isEqualTo(PositionQueryException.Reason.PROVIDER_UNAVAILABLE));
    }

    private static KisPositionApi api(RestClient.Builder builder) {
        return new KisPositionApi(
                builder.build(),
                tokenProvider(),
                APP_KEY,
                APP_SECRET,
                ACCOUNT_NUMBER,
                ACCOUNT_PRODUCT_CODE
        );
    }

    private static KisAccessTokenProvider tokenProvider() {
        return new KisAccessTokenProvider(
                () -> new AccessToken("issued-token-value", NOW.plusSeconds(3600)),
                InstantSource.fixed(NOW)
        );
    }

    private static String successResponse(String output) {
        return """
                {
                  "rt_cd":"0",
                  "output1":%s
                }
                """.formatted(output);
    }
}

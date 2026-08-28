package dev.hamm0000.autotrader.adapter.kis.query.calendar;

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
import dev.hamm0000.autotrader.adapter.kis.auth.testdouble.FakeIssuer;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.TradingDayStatusQueryException;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.TradingDayStatusObservation;
import dev.hamm0000.autotrader.trading.marketcalendar.application.port.out.TradingDayStatuses;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.CalendarPeriod;
import dev.hamm0000.autotrader.trading.marketcalendar.domain.TradingDayStatus;
import java.time.Instant;
import java.time.InstantSource;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KisTradingDayStatusApiTest {

    private static final String BASE_URL = "http://kis.test";
    private static final String HOLIDAY_PATH =
            "/uapi/domestic-stock/v1/quotations/chk-holiday";
    private static final String APP_KEY = "app-key-value";
    private static final String APP_SECRET = "app-secret-value";
    private static final Instant NOW = Instant.parse("2026-08-17T00:00:00Z");

    @Test
    void 요청한_날짜_범위의_개장_상태만_반환한다() {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(startsWith(BASE_URL + HOLIDAY_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer issued-token-value"))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header("appkey", APP_KEY))
                .andExpect(header("appsecret", APP_SECRET))
                .andExpect(header("tr_id", "CTCA0903R"))
                .andExpect(header("custtype", "P"))
                .andExpect(queryParam("BASS_DT", "20260814"))
                .andExpect(queryParam("CTX_AREA_FK", ""))
                .andExpect(queryParam("CTX_AREA_NK", ""))
                .andRespond(withSuccess("""
                        {
                          "rt_cd": "0",
                          "output": [
                            {"bass_dt": "20260813", "opnd_yn": "Y"},
                            {"bass_dt": "20260814", "opnd_yn": "Y"},
                            {"bass_dt": "20260815", "opnd_yn": "N"},
                            {"bass_dt": "20260816", "opnd_yn": "N"}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));
        KisTradingDayStatusApi api = api(builder);

        // when
        TradingDayStatuses result = api.load(period("2026-08-14", "2026-08-15"));

        // then
        assertThat(result).isEqualTo(new TradingDayStatuses(
                period("2026-08-14", "2026-08-15"),
                List.of(
                        status("2026-08-14", TradingDayStatus.OPEN),
                        status("2026-08-15", TradingDayStatus.CLOSED)
                )
        ));
        server.verify();
    }

    @Test
    void 주말에_거래일_여부가_Y여도_개장일_여부가_N이면_CLOSED를_반환한다() {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(startsWith(BASE_URL + HOLIDAY_PATH)))
                .andRespond(withSuccess("""
                        {
                          "rt_cd": "0",
                          "msg_cd": "KIOK0500",
                          "msg1": "정상처리 되었습니다.",
                          "output": [
                            {
                              "bass_dt": "20260829",
                              "wday_dvsn_cd": "07",
                              "bzdy_yn": "N",
                              "tr_day_yn": "Y",
                              "opnd_yn": "N",
                              "sttl_day_yn": "N"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));
        KisTradingDayStatusApi api = api(builder);

        // when
        TradingDayStatuses result = api.load(period("2026-08-29", "2026-08-29"));

        // then
        assertThat(result.values()).containsExactly(
                status("2026-08-29", TradingDayStatus.CLOSED)
        );
        server.verify();
    }

    @Test
    void 첫_응답이_요청한_범위를_완전히_포함하지_않으면_연속조회하지_않고_INVALID_RESPONSE로_변환한다() {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(startsWith(BASE_URL + HOLIDAY_PATH)))
                .andRespond(withSuccess("""
                        {
                          "rt_cd": "0",
                          "ctx_area_fk": "20260814",
                          "ctx_area_nk": "20260814",
                          "output": [
                            {"bass_dt": "20260814", "opnd_yn": "Y"}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON).header("tr_cont", "M"));
        KisTradingDayStatusApi api = api(builder);

        // when & then
        assertInvalidResponse(() -> api.load(period("2026-08-14", "2026-08-15")));
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
            {"rt_cd":"0"}""",
            """
            {"rt_cd":"0","output":[]}""",
            """
            {"rt_cd":"0","output":[null]}""",
            """
            {"rt_cd":"0","output":[{"opnd_yn":"Y"}]}""",
            """
            {"rt_cd":"0","output":[{"bass_dt":"20260814"}]}""",
            """
            {"rt_cd":"0","output":[{"bass_dt":"invalid","opnd_yn":"Y"}]}""",
            """
            {"rt_cd":"0","output":[{"bass_dt":"20260814","opnd_yn":"unknown"}]}""",
            """
            {"rt_cd":"0","output":{"bass_dt":"20260814","opnd_yn":"Y"}}""",
            """
            {"output":[{"bass_dt":"20260814","opnd_yn":"Y"}]}"""
    })
    void 휴장일_응답이_올바르지_않으면_INVALID_RESPONSE로_변환한다(String response) {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(startsWith(BASE_URL + HOLIDAY_PATH)))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        KisTradingDayStatusApi api = api(builder);

        // when & then
        assertInvalidResponse(() -> api.load(period("2026-08-14", "2026-08-14")));
        server.verify();
    }

    @Test
    void 휴장일_응답이_비어있으면_INVALID_RESPONSE로_변환한다() {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(startsWith(BASE_URL + HOLIDAY_PATH)))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));
        KisTradingDayStatusApi api = api(builder);

        // when & then
        assertInvalidResponse(() -> api.load(period("2026-08-14", "2026-08-14")));
        server.verify();
    }

    @Test
    void KIS가_에러_상태코드를_반환하면_PROVIDER_UNAVAILABLE로_변환한다() {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(startsWith(BASE_URL + HOLIDAY_PATH)))
                .andRespond(withServerError());
        KisTradingDayStatusApi api = api(builder);

        // when & then
        assertProviderUnavailable(() -> api.load(period("2026-08-14", "2026-08-14")));
        server.verify();
    }

    @Test
    void KIS가_실패_응답을_반환하면_PROVIDER_UNAVAILABLE로_변환한다() {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(startsWith(BASE_URL + HOLIDAY_PATH)))
                .andRespond(withSuccess("""
                        {"rt_cd":"1", "msg_cd":"ERROR", "msg1":"요청 실패"}
                        """, MediaType.APPLICATION_JSON));
        KisTradingDayStatusApi api = api(builder);

        // when & then
        assertProviderUnavailable(() -> api.load(period("2026-08-14", "2026-08-14")));
        server.verify();
    }

    @Test
    void 토큰_발급에_실패하면_PROVIDER_UNAVAILABLE로_변환한다() {
        // given
        KisTradingDayStatusApi api = apiWithFailingAuth();

        // when & then
        assertProviderUnavailable(() -> api.load(period("2026-08-14", "2026-08-14")));
    }

    private static void assertInvalidResponse(Runnable invocation) {
        assertThatThrownBy(invocation::run)
                .isInstanceOfSatisfying(TradingDayStatusQueryException.class, exception ->
                        assertThat(exception.reason()).isEqualTo(
                                TradingDayStatusQueryException.Reason.INVALID_RESPONSE
                        ));
    }

    private static void assertProviderUnavailable(Runnable invocation) {
        assertThatThrownBy(invocation::run)
                .isInstanceOfSatisfying(TradingDayStatusQueryException.class, exception ->
                        assertThat(exception.reason()).isEqualTo(
                                TradingDayStatusQueryException.Reason.PROVIDER_UNAVAILABLE
                        ));
    }

    private static CalendarPeriod period(String from, String through) {
        return new CalendarPeriod(LocalDate.parse(from), LocalDate.parse(through));
    }

    private static TradingDayStatusObservation status(
            String date,
            TradingDayStatus status
    ) {
        return new TradingDayStatusObservation(LocalDate.parse(date), status);
    }

    private static KisTradingDayStatusApi api(RestClient.Builder builder) {
        KisAccessTokenProvider tokenProvider = new KisAccessTokenProvider(
                new FakeIssuer(new AccessToken(
                        "issued-token-value",
                        NOW.plusSeconds(3600)
                )),
                InstantSource.fixed(NOW)
        );
        return new KisTradingDayStatusApi(
                builder.build(),
                tokenProvider,
                APP_KEY,
                APP_SECRET
        );
    }

    private static KisTradingDayStatusApi apiWithFailingAuth() {
        FakeIssuer issuer = new FakeIssuer();
        issuer.failWith(new KisAuthException("발급 실패"));
        KisAccessTokenProvider tokenProvider = new KisAccessTokenProvider(
                issuer,
                InstantSource.fixed(NOW)
        );
        return new KisTradingDayStatusApi(
                RestClient.builder().baseUrl(BASE_URL).build(),
                tokenProvider,
                APP_KEY,
                APP_SECRET
        );
    }
}

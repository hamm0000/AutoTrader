package dev.hamm0000.autotrader.adapter.kis.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import dev.hamm0000.autotrader.adapter.kis.auth.AccessToken;
import dev.hamm0000.autotrader.adapter.kis.auth.KisAccessTokenProvider;
import dev.hamm0000.autotrader.adapter.kis.auth.KisAuthException;
import dev.hamm0000.autotrader.trading.order.application.port.out.OrderSubmissionException;
import dev.hamm0000.autotrader.trading.order.application.port.out.OrderAcknowledgment;
import dev.hamm0000.autotrader.trading.order.domain.Order;
import dev.hamm0000.autotrader.trading.order.domain.OrderId;
import dev.hamm0000.autotrader.trading.order.domain.OrderIdempotencyKey;
import dev.hamm0000.autotrader.trading.order.domain.OrderSide;
import dev.hamm0000.autotrader.trading.order.domain.OrderState;
import dev.hamm0000.autotrader.trading.order.domain.OrderType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.InstantSource;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KisOrderApiTest {

    private static final String BASE_URL = "http://kis.test";
    private static final String ORDER_PATH = "/uapi/domestic-stock/v1/trading/order-cash";
    private static final String APP_KEY = "app-key-value";
    private static final String APP_SECRET = "app-secret-value";
    private static final String ACCOUNT_NUMBER = "account-number-value";
    private static final String ACCOUNT_PRODUCT_CODE = "01";
    private static final Instant NOW = Instant.parse("2026-09-10T00:00:00Z");

    @Test
    void 모의계좌에_시장가_매수_주문을_제출하고_접수_확인을_반환한다() {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + ORDER_PATH))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer issued-token-value"))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header("appkey", APP_KEY))
                .andExpect(header("appsecret", APP_SECRET))
                .andExpect(header("tr_id", "VTTC0012U"))
                .andExpect(header("custtype", "P"))
                .andExpect(content().json("""
                        {
                          "CANO":"account-number-value",
                          "ACNT_PRDT_CD":"01",
                          "PDNO":"005930",
                          "ORD_DVSN":"01",
                          "ORD_QTY":"138",
                          "ORD_UNPR":"0",
                          "EXCG_ID_DVSN_CD":"KRX",
                          "SLL_TYPE":"",
                          "CNDT_PRIC":""
                        }
                        """))
                .andRespond(withSuccess(successResponse(), MediaType.APPLICATION_JSON));

        // when
        OrderAcknowledgment acknowledgment = api(builder).submit(order(OrderSide.BUY));

        // then
        assertThat(acknowledgment).isEqualTo(new OrderAcknowledgment("0000123456"));
        server.verify();
    }

    @Test
    void 모의계좌에_시장가_매도_주문을_제출하고_브로커_주문번호를_반환한다() {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + ORDER_PATH))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("tr_id", "VTTC0011U"))
                .andExpect(content().json("""
                        {
                          "CANO":"account-number-value",
                          "ACNT_PRDT_CD":"01",
                          "PDNO":"005930",
                          "ORD_DVSN":"01",
                          "ORD_QTY":"138",
                          "ORD_UNPR":"0",
                          "EXCG_ID_DVSN_CD":"KRX",
                          "SLL_TYPE":"01",
                          "CNDT_PRIC":""
                        }
                        """))
                .andRespond(withSuccess(successResponse(), MediaType.APPLICATION_JSON));

        // when
        OrderAcknowledgment acknowledgment = api(builder).submit(order(OrderSide.SELL));

        // then
        assertThat(acknowledgment.brokerOrderId()).isEqualTo("0000123456");
        server.verify();
    }

    @Test
    void KIS가_주문을_명시적으로_거절하면_REJECTED로_변환한다() {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + ORDER_PATH))
                .andRespond(withSuccess("""
                        {"rt_cd":"1","msg_cd":"APBK0919","msg1":"주문 가능 금액 부족"}
                        """, MediaType.APPLICATION_JSON));

        // when & then
        assertThatThrownBy(() -> api(builder).submit(order(OrderSide.BUY)))
                .isInstanceOfSatisfying(OrderSubmissionException.class, exception -> {
                    assertThat(exception.reason())
                            .isEqualTo(OrderSubmissionException.Reason.REJECTED);
                    assertThat(exception.getMessage()).contains("APBK0919");
                });
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
            {"output":{"ODNO":"0000123456"}}""",
            """
            {"rt_cd":"0"}""",
            """
            {"rt_cd":"0","output":{}}"""
    })
    void 성공_응답을_확정할_수_없으면_RESULT_UNKNOWN으로_변환한다(String response) {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + ORDER_PATH))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        // when & then
        assertThatThrownBy(() -> api(builder).submit(order(OrderSide.BUY)))
                .isInstanceOfSatisfying(OrderSubmissionException.class, exception ->
                        assertThat(exception.reason())
                                .isEqualTo(OrderSubmissionException.Reason.RESULT_UNKNOWN));
        server.verify();
    }

    @Test
    void KIS가_호출_제한을_반환해도_주문은_재시도하지_않는다() {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + ORDER_PATH))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"rt_cd":"1","msg_cd":"EGW00201","msg1":"호출 제한"}
                                """));

        // when & then
        assertThatThrownBy(() -> api(builder).submit(order(OrderSide.BUY)))
                .isInstanceOfSatisfying(OrderSubmissionException.class, exception ->
                        assertThat(exception.reason())
                                .isEqualTo(OrderSubmissionException.Reason.RESULT_UNKNOWN));
        server.verify();
    }

    @Test
    void 주문_전송_중_HTTP_오류가_발생하면_RESULT_UNKNOWN으로_변환한다() {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + ORDER_PATH)).andRespond(withServerError());

        // when & then
        assertThatThrownBy(() -> api(builder).submit(order(OrderSide.BUY)))
                .isInstanceOfSatisfying(OrderSubmissionException.class, exception ->
                        assertThat(exception.reason())
                                .isEqualTo(OrderSubmissionException.Reason.RESULT_UNKNOWN));
        server.verify();
    }

    @Test
    void 토큰을_얻지_못하면_주문을_보내지_않고_PROVIDER_UNAVAILABLE로_변환한다() {
        // given
        KisAccessTokenProvider tokenProvider = new KisAccessTokenProvider(
                () -> { throw new KisAuthException("발급 실패"); },
                InstantSource.fixed(NOW)
        );
        KisOrderApi api = new KisOrderApi(
                RestClient.builder().baseUrl(BASE_URL).build(),
                tokenProvider,
                APP_KEY,
                APP_SECRET,
                ACCOUNT_NUMBER,
                ACCOUNT_PRODUCT_CODE
        );

        // when & then
        assertThatThrownBy(() -> api.submit(order(OrderSide.BUY)))
                .isInstanceOfSatisfying(OrderSubmissionException.class, exception ->
                        assertThat(exception.reason())
                                .isEqualTo(OrderSubmissionException.Reason.PROVIDER_UNAVAILABLE));
    }

    @Test
    void 이미_제출된_주문은_KIS에_다시_제출하지_않는다() {
        // given
        Order submitted = order(OrderSide.BUY).markSubmitted(
                "0000123456",
                NOW.plusSeconds(1)
        );

        // when & then
        assertThatThrownBy(() -> api(RestClient.builder().baseUrl(BASE_URL)).submit(submitted))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pending");
    }

    private static KisOrderApi api(RestClient.Builder builder) {
        return new KisOrderApi(
                builder.build(),
                new KisAccessTokenProvider(
                        () -> new AccessToken("issued-token-value", NOW.plusSeconds(3600)),
                        InstantSource.fixed(NOW)
                ),
                APP_KEY,
                APP_SECRET,
                ACCOUNT_NUMBER,
                ACCOUNT_PRODUCT_CODE
        );
    }

    private static Order order(OrderSide side) {
        return new Order(
                new OrderId(UUID.fromString("63df7894-b456-4bf7-85cd-706d9d32c7c9")),
                new OrderIdempotencyKey("zscore:005930:2026-09-10"),
                "005930",
                side,
                138,
                new BigDecimal("72300"),
                OrderType.MARKET,
                NOW,
                new OrderState.PendingSubmission(NOW)
        );
    }

    private static String successResponse() {
        return """
                {
                  "rt_cd":"0",
                  "msg_cd":"APBK0013",
                  "msg1":"주문 전송 완료",
                  "output":{
                    "KRX_FWDG_ORD_ORGNO":"91252",
                    "ODNO":"0000123456",
                    "ORD_TMD":"090001"
                  }
                }
                """;
    }
}

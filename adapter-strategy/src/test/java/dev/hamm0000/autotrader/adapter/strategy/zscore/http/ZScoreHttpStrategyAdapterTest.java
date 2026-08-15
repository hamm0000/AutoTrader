package dev.hamm0000.autotrader.adapter.strategy.zscore.http;

import dev.hamm0000.autotrader.trading.strategy.application.port.out.zscore.ZScoreEvaluationException;
import dev.hamm0000.autotrader.trading.strategy.application.port.out.zscore.ZScoreEvaluationRequest;
import dev.hamm0000.autotrader.trading.strategy.application.port.out.zscore.ZScoreEvaluationResult;
import dev.hamm0000.autotrader.trading.strategy.domain.StrategySignal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ZScoreHttpStrategyAdapterTest {

    private static final String BASE_URL = "http://zscore.test";

    @Test
    void 전략_평가를_요청하면_기존_계약의_경로와_필드대로_전송한다() {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + "/signal/zscore"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("\"symbol\":\"005930\"")))
                .andExpect(content().string(containsString("\"window\":2")))
                .andExpect(content().string(containsString("\"close_prices\":[99.0,101.0]")))
                .andExpect(content().string(containsString("\"now_price\":102.0")))
                .andRespond(withSuccess(responseWithSignal("SELL"), MediaType.APPLICATION_JSON));
        ZScoreHttpStrategyAdapter adapter = new ZScoreHttpStrategyAdapter(builder.build());

        // when
        adapter.evaluate(new ZScoreEvaluationRequest(
                "005930",
                2,
                List.of(99.0, 101.0),
                102.0
        ));

        // then
        server.verify();
    }

    @Test
    void 종가가_window보다_많아도_자르지_않고_모두_전송한다() {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + "/signal/zscore"))
                .andExpect(content().string(containsString("\"window\":2")))
                .andExpect(content().string(containsString(
                        "\"close_prices\":[98.0,99.0,100.0,101.0]")))
                .andRespond(withSuccess(responseWithSignal("SELL"), MediaType.APPLICATION_JSON));
        ZScoreHttpStrategyAdapter adapter = new ZScoreHttpStrategyAdapter(builder.build());

        // when
        adapter.evaluate(new ZScoreEvaluationRequest(
                "005930",
                2,
                List.of(98.0, 99.0, 100.0, 101.0),
                102.0
        ));

        // then
        server.verify();
    }

    @Test
    void 전략_서버가_계산_결과를_반환하면_값을_바꾸지_않고_전달한다() {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + "/signal/zscore"))
                .andRespond(withSuccess(responseWithSignal("SELL"), MediaType.APPLICATION_JSON));
        ZScoreHttpStrategyAdapter adapter = new ZScoreHttpStrategyAdapter(builder.build());

        // when
        ZScoreEvaluationResult result = adapter.evaluate(validRequest());

        // then
        assertThat(result.signal()).isEqualTo(StrategySignal.SELL);
        assertThat(result.zScore()).isEqualTo(2.0);
        assertThat(result.rollingMean()).isEqualTo(100.0);
        assertThat(result.rollingStddev()).isEqualTo(1.0);
        assertThat(result.reason()).isEqualTo("Z-score above upper threshold");
        server.verify();
    }


    @Test
    void 전략_서버가_알_수_없는_매매_신호를_반환하면_INVALID_RESPONSE로_변환한다() {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + "/signal/zscore"))
                .andRespond(withSuccess(responseWithSignal("UNKNOWN"), MediaType.APPLICATION_JSON));
        ZScoreHttpStrategyAdapter adapter = new ZScoreHttpStrategyAdapter(builder.build());

        // when & then
        assertThatThrownBy(() -> adapter.evaluate(validRequest()))
                .isInstanceOfSatisfying(ZScoreEvaluationException.class, exception ->
                        assertThat(exception.reason())
                                .isEqualTo(ZScoreEvaluationException.Reason.INVALID_RESPONSE));
        server.verify();
    }

    @Test
    void 전략_서버가_400으로_응답하면_INVALID_REQUEST로_변환한다() {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + "/signal/zscore"))
                .andRespond(withBadRequest().body("""
                        {"error":"close_prices size must be >= window"}
                        """).contentType(MediaType.APPLICATION_JSON));
        ZScoreHttpStrategyAdapter adapter = new ZScoreHttpStrategyAdapter(builder.build());

        // when & then
        assertThatThrownBy(() -> adapter.evaluate(validRequest()))
                .isInstanceOfSatisfying(ZScoreEvaluationException.class, exception ->
                        assertThat(exception.reason())
                                .isEqualTo(ZScoreEvaluationException.Reason.INVALID_REQUEST));
        server.verify();
    }

    @Test
    void 전략_서버가_500으로_응답하면_SERVER_UNAVAILABLE로_변환한다() {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + "/signal/zscore"))
                .andRespond(withServerError());
        ZScoreHttpStrategyAdapter adapter = new ZScoreHttpStrategyAdapter(builder.build());

        // when & then
        assertThatThrownBy(() -> adapter.evaluate(validRequest()))
                .isInstanceOfSatisfying(ZScoreEvaluationException.class, exception ->
                        assertThat(exception.reason())
                                .isEqualTo(ZScoreEvaluationException.Reason.SERVER_UNAVAILABLE));
        server.verify();
    }

    @Test
    void 전략_서버에_연결할_수_없으면_SERVER_UNAVAILABLE로_변환한다() {
        // given
        RestClient restClient = RestClient.builder()
                .requestFactory((uri, method) -> {
                    throw new IOException("연결 실패");
                })
                .build();
        ZScoreHttpStrategyAdapter adapter = new ZScoreHttpStrategyAdapter(restClient);

        // when & then
        assertThatThrownBy(() -> adapter.evaluate(validRequest()))
                .isInstanceOfSatisfying(ZScoreEvaluationException.class, exception ->
                        assertThat(exception.reason())
                                .isEqualTo(ZScoreEvaluationException.Reason.SERVER_UNAVAILABLE));
    }

    @Test
    void 전략_서버_응답에_필수_필드가_없으면_INVALID_RESPONSE로_변환한다() {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + "/signal/zscore"))
                .andRespond(withSuccess("""
                        {"signal":"SELL"}
                        """, MediaType.APPLICATION_JSON));
        ZScoreHttpStrategyAdapter adapter = new ZScoreHttpStrategyAdapter(builder.build());

        // when & then
        assertThatThrownBy(() -> adapter.evaluate(validRequest()))
                .isInstanceOfSatisfying(ZScoreEvaluationException.class, exception ->
                        assertThat(exception.reason())
                                .isEqualTo(ZScoreEvaluationException.Reason.INVALID_RESPONSE));
        server.verify();
    }

    @Test
    void 전략_서버가_해석할_수_없는_JSON을_반환하면_INVALID_RESPONSE로_변환한다() {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + "/signal/zscore"))
                .andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));
        ZScoreHttpStrategyAdapter adapter = new ZScoreHttpStrategyAdapter(builder.build());

        // when & then
        assertThatThrownBy(() -> adapter.evaluate(validRequest()))
                .isInstanceOfSatisfying(ZScoreEvaluationException.class, exception ->
                        assertThat(exception.reason())
                                .isEqualTo(ZScoreEvaluationException.Reason.INVALID_RESPONSE));
        server.verify();
    }

    private static ZScoreEvaluationRequest validRequest() {
        return new ZScoreEvaluationRequest(
                "005930",
                2,
                List.of(99.0, 101.0),
                102.0
        );
    }

    private static String responseWithSignal(String signal) {
        return """
                {
                  "signal": "%s",
                  "z_score": 2.0,
                  "rolling_mean": 100.0,
                  "rolling_stddev": 1.0,
                  "reason": "Z-score above upper threshold"
                }
                """.formatted(signal);
    }
}

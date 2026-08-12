package dev.hamm0000.autotrader.adapter.kis.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import java.net.http.HttpTimeoutException;
import java.time.Instant;
import java.time.InstantSource;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KisAccessTokenApiTest {

    private static final String BASE_URL = "http://kis.test";
    private static final String APP_KEY = "app-key-value";
    private static final String APP_SECRET = "app-secret-value";
    private static final Instant NOW = Instant.parse("2026-08-01T00:00:00Z");

    @Test
    void 발급에_성공하면_응답의_토큰_값을_돌려준다() {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + "/oauth2/tokenP"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("\"grant_type\":\"client_credentials\"")))
                .andExpect(content().string(containsString("\"appkey\":\"" + APP_KEY + "\"")))
                .andExpect(content().string(containsString("\"appsecret\":\"" + APP_SECRET + "\"")))
                .andRespond(withSuccess("""
                        {"access_token": "issued-token-value", "expires_in": 86400}
                        """, MediaType.APPLICATION_JSON));
        KisAccessTokenApi api = new KisAccessTokenApi(builder.build(), APP_KEY, APP_SECRET, InstantSource.fixed(NOW));

        // when
        AccessToken token = api.issue();

        // then
        assertThat(token.value()).isEqualTo("issued-token-value");
        server.verify();
    }

    @Test
    void KIS가_오류_상태코드를_반환하면_예외를_던진다() {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + "/oauth2/tokenP"))
                .andRespond(withUnauthorizedRequest());
        KisAccessTokenApi api = new KisAccessTokenApi(builder.build(), APP_KEY, APP_SECRET, InstantSource.fixed(NOW));

        // when & then
        assertThatThrownBy(api::issue).isInstanceOf(KisAuthException.class);
    }

    // 실제 대기 시간을 검증하지 않고 timeout 발생 시 오류 변환만 검증
    @Test
    void KIS_토큰_발급_요청의_응답_시간이_초과되면_timeout_실패로_변환한다() {
        // given
        HttpTimeoutException timeout = new HttpTimeoutException("응답 시간 초과");
        RestClient restClient = RestClient.builder()
                .requestFactory((uri, method) -> {
                    throw timeout;
                })
                .build();
        KisAccessTokenApi api = new KisAccessTokenApi(
                restClient, APP_KEY, APP_SECRET, InstantSource.fixed(NOW));

        // when & then
        assertThatThrownBy(api::issue)
                .isInstanceOfSatisfying(KisAuthException.class, exception -> {
                    assertThat(exception).hasMessageContaining("시간이 초과");
                    assertThat(exception.reason()).isEqualTo(KisAuthException.Reason.ISSUE_TIMEOUT);
                    assertThat(exception).hasRootCauseInstanceOf(HttpTimeoutException.class);
                });
    }

    @Test
    void 응답이_비어있으면_예외를_던진다() {
        // given
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + "/oauth2/tokenP"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));
        KisAccessTokenApi api = new KisAccessTokenApi(builder.build(), APP_KEY, APP_SECRET, InstantSource.fixed(NOW));

        // when & then
        assertThatThrownBy(api::issue).isInstanceOf(KisAuthException.class);
    }
}

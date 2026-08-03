package dev.hamm0000.autotrader.adapter.kis.auth;

import java.time.InstantSource;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public final class KisAccessTokenApi implements AccessTokenIssuer {

    private static final String TOKEN_PATH = "/oauth2/tokenP";
    private static final String GRANT_TYPE = "client_credentials";

    private final RestClient restClient;
    private final String appKey;
    private final String appSecret;
    private final InstantSource timeSource;

    public KisAccessTokenApi(RestClient restClient, String appKey, String appSecret, InstantSource timeSource) {
        this.restClient = restClient;
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.timeSource = timeSource;
    }

    @Override
    public AccessToken issue() {
        KisTokenResponse response;
        try {
            response = restClient.post()
                    .uri(TOKEN_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "grant_type", GRANT_TYPE,
                            "appkey", appKey,
                            "appsecret", appSecret))
                    .retrieve()
                    .body(KisTokenResponse.class);
        } catch (RestClientException e) {
            throw new KisAuthException("KIS 접근 토큰 발급 요청이 실패했습니다", e);
        }

        if (response == null) {
            throw new KisAuthException("KIS 토큰 응답이 비어 있습니다");
        }
        return response.toAccessToken(timeSource);
    }
}

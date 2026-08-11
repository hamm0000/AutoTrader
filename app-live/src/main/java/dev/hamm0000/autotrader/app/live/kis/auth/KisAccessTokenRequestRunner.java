package dev.hamm0000.autotrader.app.live.kis.auth;

import dev.hamm0000.autotrader.adapter.kis.auth.AccessToken;
import dev.hamm0000.autotrader.adapter.kis.auth.KisAccessTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

final class KisAccessTokenRequestRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KisAccessTokenRequestRunner.class);

    private final KisAccessTokenProvider provider;

    KisAccessTokenRequestRunner(KisAccessTokenProvider provider) {
        this.provider = provider;
    }

    @Override
    public void run(ApplicationArguments args) {
        AccessToken accessToken = provider.getAccessToken();
        log.info("KIS 모의투자 AccessToken 발급 성공 (accessToken={})", accessToken);
    }
}

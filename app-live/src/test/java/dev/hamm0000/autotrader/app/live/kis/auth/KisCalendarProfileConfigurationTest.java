package dev.hamm0000.autotrader.app.live.kis.auth;

import static org.assertj.core.api.Assertions.assertThat;

import dev.hamm0000.autotrader.adapter.kis.order.KisOrderApi;
import dev.hamm0000.autotrader.adapter.kis.query.KisCurrentPriceApi;
import dev.hamm0000.autotrader.adapter.kis.query.KisDailyClosingPriceApi;
import dev.hamm0000.autotrader.adapter.kis.query.account.KisPositionApi;
import dev.hamm0000.autotrader.adapter.kis.query.calendar.KisTradingDayStatusApi;
import dev.hamm0000.autotrader.app.live.kis.KisPaperProperties;
import dev.hamm0000.autotrader.app.live.kis.KisProductionProperties;
import dev.hamm0000.autotrader.app.live.kis.account.KisPaperAccountProperties;
import dev.hamm0000.autotrader.app.live.kis.order.KisPaperOrderConfiguration;
import dev.hamm0000.autotrader.app.live.kis.query.KisPaperQueryConfiguration;
import dev.hamm0000.autotrader.app.live.kis.query.account.KisPaperAccountQueryConfiguration;
import dev.hamm0000.autotrader.app.live.kis.query.calendar.KisCalendarQueryConfiguration;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

class KisCalendarProfileConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(RestClient.Builder.class, RestClient::builder)
            .withUserConfiguration(
                    KisPaperAuthConfiguration.class,
                    KisProductionAuthConfiguration.class,
                    KisPaperQueryConfiguration.class,
                    KisPaperAccountQueryConfiguration.class,
                    KisPaperOrderConfiguration.class,
                    KisCalendarQueryConfiguration.class
            );

    @Test
    void 캘린더_갱신_프로필은_실전투자_인증과_휴장일_조회만_조립한다() {
        contextRunner
                .withBean(
                        KisProductionProperties.class,
                        KisCalendarProfileConfigurationTest::productionProperties
                )
                .withInitializer(context -> context.getEnvironment()
                        .setActiveProfiles("market-calendar-refresh"))
                .run(context -> {
                    assertThat(context).hasSingleBean(KisTradingDayStatusApi.class);
                    assertThat(context).doesNotHaveBean(KisCurrentPriceApi.class);
                    assertThat(context).doesNotHaveBean(KisDailyClosingPriceApi.class);
                    assertThat(context).doesNotHaveBean(KisPositionApi.class);
                    assertThat(context).doesNotHaveBean(KisOrderApi.class);
                    assertThat(context).doesNotHaveBean(KisAccessTokenRequestRunner.class);
                    assertThat(context).hasBean("kisProductionAccessTokenProvider");
                    assertThat(context).doesNotHaveBean("kisPaperAccessTokenProvider");
                });
    }

    @Test
    void 기본_프로필은_모의투자_인증과_시세_조회만_조립한다() {
        contextRunner
                .withBean(
                        KisPaperProperties.class,
                        KisCalendarProfileConfigurationTest::paperProperties
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(KisCurrentPriceApi.class);
                    assertThat(context).hasSingleBean(KisDailyClosingPriceApi.class);
                    assertThat(context).doesNotHaveBean(KisPositionApi.class);
                    assertThat(context).doesNotHaveBean(KisOrderApi.class);
                    assertThat(context).doesNotHaveBean(KisTradingDayStatusApi.class);
                    assertThat(context).hasSingleBean(KisAccessTokenRequestRunner.class);
                    assertThat(context).hasBean("kisPaperAccessTokenProvider");
                    assertThat(context).doesNotHaveBean("kisProductionAccessTokenProvider");
                });
    }

    @Test
    void 모의매매_프로필은_모의계좌_잔고조회와_주문제출을_추가로_조립한다() {
        contextRunner
                .withBean(
                        KisPaperProperties.class,
                        KisCalendarProfileConfigurationTest::paperProperties
                )
                .withBean(
                        KisPaperAccountProperties.class,
                        KisCalendarProfileConfigurationTest::paperAccountProperties
                )
                .withInitializer(context -> context.getEnvironment()
                        .setActiveProfiles("paper-trading"))
                .run(context -> {
                    assertThat(context).hasSingleBean(KisCurrentPriceApi.class);
                    assertThat(context).hasSingleBean(KisDailyClosingPriceApi.class);
                    assertThat(context).hasSingleBean(KisPositionApi.class);
                    assertThat(context).hasSingleBean(KisOrderApi.class);
                    assertThat(context).doesNotHaveBean(KisTradingDayStatusApi.class);
                });
    }

    private static KisPaperProperties paperProperties() {
        return new KisPaperProperties(
                URI.create("https://openapivts.koreainvestment.com:29443"),
                "test-paper-app-key",
                "test-paper-app-secret",
                Duration.ofSeconds(5)
        );
    }

    private static KisProductionProperties productionProperties() {
        return new KisProductionProperties(
                URI.create("https://openapi.koreainvestment.com:9443"),
                "test-production-app-key",
                "test-production-app-secret",
                Duration.ofSeconds(5)
        );
    }

    private static KisPaperAccountProperties paperAccountProperties() {
        return new KisPaperAccountProperties(
                "test-account-number",
                "test-product-code"
        );
    }
}

package dev.hamm0000.autotrader.app.live.order;

import static org.assertj.core.api.Assertions.assertThat;

import dev.hamm0000.autotrader.trading.order.application.port.in.PlaceOrder;
import dev.hamm0000.autotrader.trading.order.application.port.out.FindOrder;
import dev.hamm0000.autotrader.trading.order.application.port.out.GenerateOrderId;
import dev.hamm0000.autotrader.trading.order.application.port.out.OrderAcknowledgment;
import dev.hamm0000.autotrader.trading.order.application.port.out.SaveOrder;
import dev.hamm0000.autotrader.trading.order.application.port.out.SubmitOrder;
import java.time.InstantSource;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class OrderConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(FindOrder.class, () -> idempotencyKey -> Optional.empty())
            .withBean(SaveOrder.class, () -> order -> { })
            .withBean(SubmitOrder.class, () -> order -> new OrderAcknowledgment("broker-order"))
            .withBean(InstantSource.class, InstantSource::system)
            .withUserConfiguration(OrderConfiguration.class);

    @Test
    void 모의매매_프로필은_주문번호_생성과_주문_제출_유즈케이스를_조립한다() {
        contextRunner
                .withInitializer(context -> context.getEnvironment()
                        .setActiveProfiles("paper-trading"))
                .run(context -> {
                    assertThat(context).hasSingleBean(GenerateOrderId.class);
                    assertThat(context).hasSingleBean(PlaceOrder.class);
                });
    }

    @Test
    void 기본_프로필은_주문_제출_유즈케이스를_조립하지_않는다() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(GenerateOrderId.class);
            assertThat(context).doesNotHaveBean(PlaceOrder.class);
        });
    }
}

package dev.hamm0000.autotrader.app.live.order;

import dev.hamm0000.autotrader.trading.order.application.port.in.PlaceOrder;
import dev.hamm0000.autotrader.trading.order.application.port.out.FindOrder;
import dev.hamm0000.autotrader.trading.order.application.port.out.GenerateOrderId;
import dev.hamm0000.autotrader.trading.order.application.port.out.SaveOrder;
import dev.hamm0000.autotrader.trading.order.application.port.out.SubmitOrder;
import dev.hamm0000.autotrader.trading.order.application.service.OrderPlacementService;
import java.time.InstantSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("paper-trading")
@Configuration(proxyBeanMethods = false)
class OrderConfiguration {

    @Bean
    GenerateOrderId generateOrderId() {
        return new UuidOrderIdGenerator();
    }

    @Bean
    PlaceOrder placeOrder(
            FindOrder findOrder,
            SaveOrder saveOrder,
            SubmitOrder submitOrder,
            GenerateOrderId generateOrderId,
            InstantSource instantSource
    ) {
        return new OrderPlacementService(
                findOrder,
                saveOrder,
                submitOrder,
                generateOrderId,
                instantSource
        );
    }
}

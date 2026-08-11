package dev.hamm0000.autotrader.app.live;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AutoTraderLiveApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutoTraderLiveApplication.class, args);
    }
}

package com.thesis.stocktradingsimulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching     // Turns on Spring Boot's memory cache
@EnableScheduling  // Allows us to set a timer to clear the cache
public class StockTradingSimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockTradingSimulatorApplication.class, args);
    }

}

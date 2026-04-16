package com.thesis.stocktradingsimulator;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

class StockTradingSimulatorApplicationTests extends BaseIntegrationTest {

    @Test
    void contextLoadsAndDefaultUserIsCreated() {
        assertNotNull(defaultTestUser, "The default user should have been created by the base class");

        assertEquals(0, new BigDecimal("10000.00").compareTo(defaultTestUser.getCashBalance()),
                "The default user should automatically start with $10,000");

        assertNotNull(defaultPortfolio, "The user's portfolio should automatically exist");
    }
}
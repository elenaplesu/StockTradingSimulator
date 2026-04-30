package com.thesis.stocktradingsimulator.model;

import com.thesis.stocktradingsimulator.exception.InsufficientSharesException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HoldingTest {

    private Holding holding;
    private Portfolio dummyPortfolio;

    @BeforeEach
    void setUp() {
        dummyPortfolio = new Portfolio();
        holding = new Holding(dummyPortfolio, "AAPL", 10, new BigDecimal("100.00"));
    }

    @Test
    void addShares_ShouldIncreaseQuantityAndCalculateNewAveragePrice() {

        holding.addShares(10, new BigDecimal("50.00"));

        assertEquals(20, holding.getQuantity(), "Quantity should increase to 20");

        assertEquals(0, new BigDecimal("75.00").compareTo(holding.getAverageBuyPrice()), "Average buy price should be $75.00");
    }

    @Test
    void addShares_ShouldThrowException_WhenQuantityIsZeroOrNegative() {
        assertThrows(IllegalArgumentException.class, () -> {
            holding.addShares(0, new BigDecimal("100.00"));
        }, "Should not allow adding 0 shares");

        assertThrows(IllegalArgumentException.class, () -> {
            holding.addShares(-5, new BigDecimal("100.00"));
        }, "Should not allow adding negative shares");
    }

    @Test
    void removeShares_ShouldDecreaseQuantity_AndKeepAveragePriceUnchanged() {
        holding.removeShares(4);

        assertEquals(6, holding.getQuantity(), "Quantity should decrease to 6");
        assertEquals(0, new BigDecimal("100.00").compareTo(holding.getAverageBuyPrice()), "Average buy price should remain $100.00");
    }

    @Test
    void removeShares_ShouldThrowException_WhenSellingMoreThanOwned() {
        assertThrows(InsufficientSharesException.class, () -> {
            holding.removeShares(11);
        }, "Should not allow selling more shares than currently owned");
    }

    @Test
    void removeShares_ShouldThrowException_WhenQuantityIsZeroOrNegative() {
        assertThrows(IllegalArgumentException.class, () -> {
            holding.removeShares(0);
        }, "Should not allow selling 0 shares");

        assertThrows(IllegalArgumentException.class, () -> {
            holding.removeShares(-2);
        }, "Should not allow selling negative shares");
    }
}
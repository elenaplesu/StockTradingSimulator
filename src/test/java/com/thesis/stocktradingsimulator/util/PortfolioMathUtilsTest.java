package com.thesis.stocktradingsimulator.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PortfolioMathUtilsTest {

    @Test
    void calculateROI_ShouldReturnPositivePercentage_WhenProfitable() {
        BigDecimal currentValue = new BigDecimal("150.00");
        BigDecimal costBasis = new BigDecimal("100.00");

        BigDecimal roi = PortfolioMathUtils.calculateROI(currentValue, costBasis);

        assertEquals(0, new BigDecimal("50.00").compareTo(roi), "ROI should be exactly 50.00%");
    }

    @Test
    void calculateROI_ShouldReturnNegativePercentage_WhenLosing() {
        BigDecimal currentValue = new BigDecimal("75.00");
        BigDecimal costBasis = new BigDecimal("100.00");

        BigDecimal roi = PortfolioMathUtils.calculateROI(currentValue, costBasis);

        assertEquals(0, new BigDecimal("-25.00").compareTo(roi), "ROI should be exactly -25.00%");
    }

    @Test
    void calculateROI_ShouldSafelyReturnZero_WhenCostBasisIsZero() {
        BigDecimal roi = PortfolioMathUtils.calculateROI(new BigDecimal("50.00"), BigDecimal.ZERO);
        assertEquals(0, BigDecimal.ZERO.compareTo(roi), "Should not throw DivideByZero exception");
    }

    @Test
    void calculateNormalizedHHI_ShouldReturn5000_ForPerfectlyDividedPortfolio() {
        List<BigDecimal> weights = List.of(new BigDecimal("0.50"), new BigDecimal("0.50"));
        BigDecimal hhi = PortfolioMathUtils.calculateNormalizedHHI(weights);
        assertEquals(0, new BigDecimal("5000.00").compareTo(hhi));
    }

    @Test
    void calculateNormalizedHHI_ShouldReturn10000_ForSingleAsset() {
        List<BigDecimal> weights = List.of(new BigDecimal("1.00"));
        BigDecimal hhi = PortfolioMathUtils.calculateNormalizedHHI(weights);
        assertEquals(0, new BigDecimal("10000.00").compareTo(hhi));
    }

    @Test
    void calculateCrossSectionalVariance_ShouldReturnZero_WhenAllReturnsAreIdentical() {
        List<BigDecimal> returns = List.of(new BigDecimal("10.00"), new BigDecimal("10.00"));

        BigDecimal variance = PortfolioMathUtils.calculateCrossSectionalVariance(returns);

        assertEquals(0, BigDecimal.ZERO.compareTo(variance));
    }

    @Test
    void calculateCrossSectionalVariance_ShouldCalculateCorrectly() {
        List<BigDecimal> returns = List.of(new BigDecimal("10.00"), new BigDecimal("20.00"));
        BigDecimal variance = PortfolioMathUtils.calculateCrossSectionalVariance(returns);
        assertEquals(0, new BigDecimal("25.00").compareTo(variance));
    }
}
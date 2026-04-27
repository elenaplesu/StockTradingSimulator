package com.thesis.stocktradingsimulator.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class PortfolioMathUtils {

    public static BigDecimal calculateROI(BigDecimal currentValue, BigDecimal costBasis) {
        if (costBasis.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return currentValue.subtract(costBasis)
                .divide(costBasis, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateNormalizedHHI(List<BigDecimal> weights) {
        if (weights == null || weights.isEmpty()) return BigDecimal.ZERO;
        BigDecimal hhi = BigDecimal.ZERO;
        for (BigDecimal weight : weights) {
            hhi = hhi.add(weight.pow(2));
        }
        return hhi.multiply(new BigDecimal("10000")).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateCrossSectionalVariance(List<BigDecimal> individualReturns) {
        if (individualReturns.isEmpty()) return BigDecimal.ZERO;

        BigDecimal sumReturns = BigDecimal.ZERO;
        for (BigDecimal r : individualReturns) {
            sumReturns = sumReturns.add(r);
        }

        BigDecimal count = BigDecimal.valueOf(individualReturns.size());
        BigDecimal meanReturn = sumReturns.divide(count, 4, RoundingMode.HALF_UP);

        BigDecimal varianceSum = BigDecimal.ZERO;
        for (BigDecimal r : individualReturns) {
            BigDecimal difference = r.subtract(meanReturn);
            varianceSum = varianceSum.add(difference.pow(2));
        }
        return varianceSum.divide(count, 4, RoundingMode.HALF_UP);
    }
}
package com.thesis.stocktradingsimulator.dto;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioAnalyticsDTO (
     BigDecimal cashBalance,
     BigDecimal totalEquity,
     BigDecimal netWorth,
     BigDecimal aggregateROI,
     BigDecimal hhi,
     BigDecimal crossSectionalVariance,
     List<EnrichedHoldingDTO> holdings)
{}
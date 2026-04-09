package com.thesis.stocktradingsimulator.dto;

import java.math.BigDecimal;

public record EnrichedHoldingDTO(
        String symbol,
        int quantity,
        BigDecimal averageBuyPrice,
        BigDecimal currentPrice,
        BigDecimal totalValue,
        BigDecimal weightPercentage,
        BigDecimal returnOnInvestment
) {}
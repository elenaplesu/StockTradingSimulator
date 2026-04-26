package com.thesis.stocktradingsimulator.dto;

import java.math.BigDecimal;

public record HoldingDTO(
        String symbol,
        int quantity,
        BigDecimal averageBuyPrice
) {}
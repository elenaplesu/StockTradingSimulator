package com.thesis.stocktradingsimulator.dto;
import java.math.BigDecimal;

public record ChartPoint(long timestamp, String time, BigDecimal price) {}
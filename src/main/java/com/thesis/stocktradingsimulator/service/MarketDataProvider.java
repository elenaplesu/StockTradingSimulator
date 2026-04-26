package com.thesis.stocktradingsimulator.service;

import com.thesis.stocktradingsimulator.dto.ChartPoint;
import com.thesis.stocktradingsimulator.model.StockQuote;
import java.util.List;

public interface MarketDataProvider {
    StockQuote getLivePrice(String symbol);
    List<ChartPoint> getStockHistory(String symbol, String range);
}
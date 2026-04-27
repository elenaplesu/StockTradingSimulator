package com.thesis.stocktradingsimulator.model;

import java.math.BigDecimal;

public class StockQuote {
    private String symbol;
    private BigDecimal currentPrice;

    public StockQuote(String symbol, BigDecimal currentPrice) {
        this.symbol = symbol;
        this.currentPrice = currentPrice;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }

}

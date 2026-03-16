package com.thesis.stocktradingsimulator.dto;

public class EnrichedHoldingDTO {
    private String symbol;
    private int quantity;
    private double averageBuyPrice;
    private double currentPrice;

    // The Math Fields
    private double totalValue;
    private double weightPercentage;
    private double returnOnInvestment;

    public EnrichedHoldingDTO(String symbol, int quantity, double averageBuyPrice, double currentPrice, double totalValue, double weightPercentage, double returnOnInvestment) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.averageBuyPrice = averageBuyPrice;
        this.currentPrice = currentPrice;
        this.totalValue = totalValue;
        this.weightPercentage = weightPercentage;
        this.returnOnInvestment = returnOnInvestment;
    }

    // Getters
    public String getSymbol() { return symbol; }
    public int getQuantity() { return quantity; }
    public double getAverageBuyPrice() { return averageBuyPrice; }
    public double getCurrentPrice() { return currentPrice; }
    public double getTotalValue() { return totalValue; }
    public double getWeightPercentage() { return weightPercentage; }
    public double getReturnOnInvestment() { return returnOnInvestment; }
}
package com.thesis.stocktradingsimulator.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "holdings", indexes = {
        @Index(name = "idx_holding_portfolio_symbol", columnList = "portfolio_id, symbol", unique = true)
})
public class Holding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "portfolio_id", nullable = false)
    @JsonIgnore
    private Portfolio portfolio;

    @Version
    private Long version=0L;

    @Column(nullable = false)
    private String symbol;

    private int quantity;

    @Column(name = "average_buy_price", precision = 19, scale = 2)
    private BigDecimal averageBuyPrice;

    public Holding() {}

    public Holding(Portfolio portfolio, String symbol, int quantity, BigDecimal averageBuyPrice) {
        this.portfolio = portfolio;
        this.symbol = symbol;
        this.quantity = quantity;
        this.averageBuyPrice = averageBuyPrice;
    }
    public void addShares(int additionalQuantity, BigDecimal purchasePrice) {
        if (additionalQuantity <= 0) throw new IllegalArgumentException("Quantity must be positive");

        BigDecimal currentQty = BigDecimal.valueOf(this.quantity);
        BigDecimal addedQty = BigDecimal.valueOf(additionalQuantity);

        BigDecimal totalSpentPreviously = currentQty.multiply(this.averageBuyPrice);
        BigDecimal newCost = addedQty.multiply(purchasePrice);
        BigDecimal newTotalSpent = totalSpentPreviously.add(newCost);

        this.quantity += additionalQuantity;
        this.averageBuyPrice = newTotalSpent.divide(BigDecimal.valueOf(this.quantity), 2, RoundingMode.HALF_UP);
    }
    public void removeShares(int amountToSell) {
        if (amountToSell <= 0) throw new IllegalArgumentException("Amount to sell must be positive");
        if (this.quantity < amountToSell) throw new IllegalArgumentException("Cannot sell more shares than owned");

        this.quantity -= amountToSell;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Portfolio getPortfolio() { return portfolio; }
    public void setPortfolio(Portfolio portfolio) { this.portfolio = portfolio; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getAverageBuyPrice() { return averageBuyPrice; }
    public void setAverageBuyPrice(BigDecimal averageBuyPrice) { this.averageBuyPrice = averageBuyPrice; }
}

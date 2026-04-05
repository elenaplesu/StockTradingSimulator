package com.thesis.stocktradingsimulator.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "portfolio_id", nullable = false)
    @JsonIgnore
    private Portfolio portfolio;

    @Column(updatable = false)
    private LocalDateTime timestamp;

    private String type; // "BUY" / "SELL"
    private String symbol;
    private int quantity;
    private double executionPrice;

    public Transaction() {}

    public Transaction(Portfolio portfolio, String type, String symbol, int quantity, double executionPrice) {
        this.portfolio = portfolio;
        this.type = type;
        this.symbol = symbol;
        this.quantity = quantity;
        this.executionPrice = executionPrice;
        this.timestamp = LocalDateTime.now(ZoneOffset.UTC);
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Portfolio getPortfolio() { return portfolio; }
    public void setPortfolio(Portfolio portfolio) { this.portfolio = portfolio; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getExecutionPrice() { return executionPrice; }
    public void setExecutionPrice(double executionPrice) { this.executionPrice = executionPrice; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
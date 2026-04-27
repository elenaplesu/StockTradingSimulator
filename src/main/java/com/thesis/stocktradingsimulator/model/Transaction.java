package com.thesis.stocktradingsimulator.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transaction_portfolio_time", columnList = "portfolio_id, timestamp")
})
public class Transaction {

    public enum TransactionType {
        BUY, SELL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "portfolio_id", nullable = false)
    @JsonIgnore
    private Portfolio portfolio;

    @Column(updatable = false)
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TransactionType type;

    private String symbol;
    private int quantity;
    private BigDecimal executionPrice;

    public Transaction() {}

    public Transaction(Portfolio portfolio, TransactionType type, String symbol, int quantity, BigDecimal executionPrice) {
        this.portfolio = portfolio;
        this.type = type;
        this.symbol = symbol;
        this.quantity = quantity;
        this.executionPrice = executionPrice;
    }
    @PrePersist
    private void onCreate() {
        this.timestamp = LocalDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Portfolio getPortfolio() { return portfolio; }
    public void setPortfolio(Portfolio portfolio) { this.portfolio = portfolio; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getExecutionPrice() { return executionPrice; }
    public void setExecutionPrice(BigDecimal executionPrice) { this.executionPrice = executionPrice; }

    public LocalDateTime getTimestamp() { return timestamp; }
}
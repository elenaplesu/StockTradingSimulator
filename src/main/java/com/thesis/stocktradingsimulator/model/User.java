package com.thesis.stocktradingsimulator.model;

import com.thesis.stocktradingsimulator.exception.InsufficientFundsException;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(precision = 19, scale = 4)
    private BigDecimal cashBalance=new BigDecimal("10000.0000");;

    public User() {}

    public User(String username, String password, BigDecimal cashBalance) {
        this.username = username;
        this.password = password;
        this.cashBalance = cashBalance;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getVersion() { return version; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public BigDecimal getCashBalance() {
        if (this.cashBalance == null) return BigDecimal.ZERO;
        return cashBalance;
    }

    public void addFunds(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Cannot add negative funds");
        this.cashBalance = this.cashBalance.add(amount);
    }

    public void deductFunds(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Cannot deduct negative funds");
        if (this.cashBalance.compareTo(amount) < 0) throw new InsufficientFundsException("Insufficient funds");
        this.cashBalance = this.cashBalance.subtract(amount);
    }
}
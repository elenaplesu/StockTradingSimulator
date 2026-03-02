package com.thesis.stocktradingsimulator.model;
import jakarta.persistence.*;

@Entity
@Table(name = "portfolio")

public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // This links the Portfolio directly to a User
    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    private double totalValue;

    public Portfolio() {}

    public Portfolio(User user, double totalValue) {
        this.user = user;
        this.totalValue = totalValue;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public double getTotalValue() { return totalValue; }
    public void setTotalValue(double totalValue) { this.totalValue = totalValue; }
}

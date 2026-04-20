package com.thesis.stocktradingsimulator.model;

import com.thesis.stocktradingsimulator.exception.InsufficientFundsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("testUser", "password", new BigDecimal("10000.00"));
    }

    @Test
    void addFunds_ShouldIncreaseBalance_WhenAmountIsPositive() {
        user.addFunds(new BigDecimal("5000.00"));
        assertEquals(0, new BigDecimal("15000.00").compareTo(user.getCashBalance()));
    }

    @Test
    void addFunds_ShouldThrowException_WhenAmountIsNegative() {
        assertThrows(IllegalArgumentException.class, () -> {
            user.addFunds(new BigDecimal("-10.00"));
        });
    }

    @Test
    void deductFunds_ShouldDecreaseBalance_WhenFundsAreAvailable() {
        user.deductFunds(new BigDecimal("4000.00"));
        assertEquals(0, new BigDecimal("6000.00").compareTo(user.getCashBalance()));
    }

    @Test
    void deductFunds_ShouldThrowInsufficientFundsException_WhenBalanceIsTooLow() {

        assertThrows(InsufficientFundsException.class, () -> {
            user.deductFunds(new BigDecimal("200000.00"));
        });
    }

    @Test
    void deductFunds_ShouldThrowException_WhenAmountIsNegative() {
        assertThrows(IllegalArgumentException.class, () -> {
            user.deductFunds(new BigDecimal("-50.00"));
        });
    }

    @Test
    void getCashBalance_ShouldReturn10kByDefault() {
        User newUser = new User();
        assertEquals(0,new BigDecimal("10000.00").compareTo(newUser.getCashBalance()));
    }
}
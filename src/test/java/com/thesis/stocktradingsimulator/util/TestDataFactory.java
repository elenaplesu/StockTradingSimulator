package com.thesis.stocktradingsimulator.util;

import com.thesis.stocktradingsimulator.model.Portfolio;
import com.thesis.stocktradingsimulator.model.User;
import java.math.BigDecimal;

public class TestDataFactory {

    public static User createStandardMockUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testStudent");
        user.setPassword("hashedPassword");
        return user;
    }

    public static Portfolio createStandardMockPortfolio(User user) {
        Portfolio portfolio = new Portfolio();
        portfolio.setId(1L);
        portfolio.setUser(user);
        return portfolio;
    }
}
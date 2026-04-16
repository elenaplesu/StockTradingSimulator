package com.thesis.stocktradingsimulator.repository;

import com.thesis.stocktradingsimulator.BaseIntegrationTest;
import com.thesis.stocktradingsimulator.model.Portfolio;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PortfolioRepositoryTest extends BaseIntegrationTest {

    @Test
    void findByUserId_ShouldReturnPortfolio_WhenUserExists() {
        Long userId = defaultTestUser.getId();
        Optional<Portfolio> foundPortfolio = portfolioRepository.findByUserId(userId);

        assertTrue(foundPortfolio.isPresent(), "Portfolio should be found for the existing user ID");
        assertEquals(defaultPortfolio.getId(), foundPortfolio.get().getId(), "Should return the correct portfolio instance");
        assertEquals(userId, foundPortfolio.get().getUser().getId(), "The linked user ID should match");
    }

    @Test
    void findByUserId_ShouldReturnEmpty_WhenUserDoesNotExist() {
        Optional<Portfolio> foundPortfolio = portfolioRepository.findByUserId(9999L);

        assertTrue(foundPortfolio.isEmpty(), "Should return empty for a non-existent user ID");
    }
}
package com.thesis.stocktradingsimulator.repository;

import com.thesis.stocktradingsimulator.BaseIntegrationTest;
import com.thesis.stocktradingsimulator.model.Holding;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class HoldingRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private HoldingRepository holdingRepository;

    @Test
    void findByPortfolioIdAndSymbol_ShouldReturnHolding_WhenItExists() {
        Holding apple = new Holding(defaultPortfolio, "AAPL", 10, new BigDecimal("150.00"));
        holdingRepository.save(apple);

        Optional<Holding> found = holdingRepository.findByPortfolioIdAndSymbol(defaultPortfolio.getId(), "AAPL");

        assertTrue(found.isPresent());
        assertEquals("AAPL", found.get().getSymbol());
        assertEquals(10, found.get().getQuantity());
    }

    @Test
    void findByPortfolioIdAndSymbol_ShouldReturnEmpty_WhenSymbolIsWrong() {

        Holding apple = new Holding(defaultPortfolio, "AAPL", 10, new BigDecimal("150.00"));
        holdingRepository.save(apple);

        Optional<Holding> found = holdingRepository.findByPortfolioIdAndSymbol(defaultPortfolio.getId(), "TSLA");

        assertTrue(found.isEmpty());
    }

    @Test
    void findByPortfolioId_ShouldReturnAllHoldingsForUser() {

        holdingRepository.save(new Holding(defaultPortfolio, "AAPL", 10, new BigDecimal("150.00")));
        holdingRepository.save(new Holding(defaultPortfolio, "TSLA", 5, new BigDecimal("200.00")));

        List<Holding> holdings = holdingRepository.findByPortfolioId(defaultPortfolio.getId());

        assertEquals(2, holdings.size(), "Should find both stocks in the portfolio");
        assertTrue(holdings.stream().anyMatch(h -> h.getSymbol().equals("AAPL")));
        assertTrue(holdings.stream().anyMatch(h -> h.getSymbol().equals("TSLA")));
    }
}
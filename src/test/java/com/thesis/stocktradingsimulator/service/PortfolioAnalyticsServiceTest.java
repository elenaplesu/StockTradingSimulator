package com.thesis.stocktradingsimulator.service;

import com.thesis.stocktradingsimulator.dto.PortfolioAnalyticsDTO;
import com.thesis.stocktradingsimulator.model.Holding;
import com.thesis.stocktradingsimulator.model.Portfolio;
import com.thesis.stocktradingsimulator.model.StockQuote;
import com.thesis.stocktradingsimulator.model.User;
import com.thesis.stocktradingsimulator.repository.HoldingRepository;
import com.thesis.stocktradingsimulator.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioAnalyticsServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private MarketDataService marketDataService;

    @InjectMocks
    private PortfolioAnalyticsService analyticsService;

    private User mockUser;
    private Portfolio mockPortfolio;

    @BeforeEach
    void setUp() {
        mockUser = TestDataFactory.createStandardMockUser();
        mockPortfolio = TestDataFactory.createStandardMockPortfolio(mockUser);
    }

    @Test
    void testGenerateAnalytics_PerfectDiversificationHHI() {
        Holding apple = new Holding(mockPortfolio, "AAPL", 1, new BigDecimal("50.00"));
        Holding tesla = new Holding(mockPortfolio, "TSLA", 1, new BigDecimal("50.00"));

        when(userService.getUserById(1L)).thenReturn(mockUser);
        when(userService.getPortfolioByUserId(1L)).thenReturn(mockPortfolio);
        when(holdingRepository.findByPortfolioId(1L)).thenReturn(List.of(apple, tesla));

        when(marketDataService.getLivePrice("AAPL")).thenReturn(new StockQuote("AAPL", new BigDecimal("50.00")));
        when(marketDataService.getLivePrice("TSLA")).thenReturn(new StockQuote("TSLA", new BigDecimal("50.00")));

        PortfolioAnalyticsDTO result = analyticsService.generateAnalytics(1L);

        BigDecimal expectedEquity = new BigDecimal("100.00");
        BigDecimal expectedNetWorth = mockUser.getCashBalance().add(expectedEquity);

        assertEquals(0, expectedEquity.compareTo(result.totalEquity()), "Total equity should be exactly 100");
        assertEquals(0, expectedNetWorth.compareTo(result.netWorth()), "Net worth calculation is incorrect");
        assertEquals(0, new BigDecimal("5000.00").compareTo(result.hhi()), "A 50/50 portfolio should have an HHI of exactly 5000");
    }

    @Test
    void testGenerateAnalytics_ZeroDiversificationHHI() {
        Holding nvidia = new Holding(mockPortfolio, "NVDA", 10, new BigDecimal("50.00"));

        when(userService.getUserById(1L)).thenReturn(mockUser);
        when(userService.getPortfolioByUserId(1L)).thenReturn(mockPortfolio);
        when(holdingRepository.findByPortfolioId(1L)).thenReturn(List.of(nvidia));

        when(marketDataService.getLivePrice("NVDA")).thenReturn(new StockQuote("NVDA", new BigDecimal("50.00")));

        PortfolioAnalyticsDTO result = analyticsService.generateAnalytics(1L);

        assertEquals(0, new BigDecimal("10000.00").compareTo(result.hhi()), "All-in portfolio must have 10,000 HHI");
    }

    @Test
    void testGenerateAnalytics_NegativeROI() {
        Holding apple = new Holding(mockPortfolio, "AAPL", 1, new BigDecimal("100.00"));

        when(userService.getUserById(1L)).thenReturn(mockUser);
        when(userService.getPortfolioByUserId(1L)).thenReturn(mockPortfolio);
        when(holdingRepository.findByPortfolioId(1L)).thenReturn(List.of(apple));

        when(marketDataService.getLivePrice("AAPL")).thenReturn(new StockQuote("AAPL", new BigDecimal("50.00")));

        PortfolioAnalyticsDTO result = analyticsService.generateAnalytics(1L);

        assertEquals(0, new BigDecimal("-50.00").compareTo(result.aggregateROI()), "Negative ROI was not calculated correctly");
    }

    @Test
    void testGenerateAnalytics_EmptyPortfolio() {
        when(userService.getUserById(1L)).thenReturn(mockUser);
        when(userService.getPortfolioByUserId(1L)).thenReturn(mockPortfolio);
        when(holdingRepository.findByPortfolioId(1L)).thenReturn(List.of());

        PortfolioAnalyticsDTO result = analyticsService.generateAnalytics(1L);

        assertEquals(0, BigDecimal.ZERO.compareTo(result.totalEquity()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.hhi()));
        assertEquals(0, mockUser.getCashBalance().compareTo(result.netWorth()));
    }
}
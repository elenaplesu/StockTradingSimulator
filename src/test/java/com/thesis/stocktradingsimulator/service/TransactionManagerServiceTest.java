package com.thesis.stocktradingsimulator.service;

import com.thesis.stocktradingsimulator.exception.InsufficientFundsException;
import com.thesis.stocktradingsimulator.exception.InsufficientSharesException;
import com.thesis.stocktradingsimulator.model.Holding;
import com.thesis.stocktradingsimulator.model.Portfolio;
import com.thesis.stocktradingsimulator.model.StockQuote;
import com.thesis.stocktradingsimulator.model.Transaction;
import com.thesis.stocktradingsimulator.model.User;
import com.thesis.stocktradingsimulator.repository.HoldingRepository;
import com.thesis.stocktradingsimulator.repository.PortfolioRepository;
import com.thesis.stocktradingsimulator.repository.TransactionRepository;
import com.thesis.stocktradingsimulator.repository.UserRepository;
import com.thesis.stocktradingsimulator.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionManagerServiceTest {

    // 1. Updated to use the Interface!
    @Mock private MarketDataProvider marketDataProvider;
    @Mock private UserRepository userRepository;
    @Mock private PortfolioRepository portfolioRepository;
    @Mock private HoldingRepository holdingRepository;
    @Mock private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionManagerService transactionManagerService;

    private User mockUser;
    private Portfolio mockPortfolio;

    @BeforeEach
    void setUp() {
        mockUser = TestDataFactory.createStandardMockUser();
        mockPortfolio = TestDataFactory.createStandardMockPortfolio(mockUser);
    }

    @Test
    void executeBuy_ShouldBlockTrade_WhenInsufficientFunds() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(mockPortfolio));
        when(marketDataProvider.getLivePrice("AAPL")).thenReturn(new StockQuote("AAPL", new BigDecimal("50.00")));

        assertThrows(InsufficientFundsException.class, () -> {
            transactionManagerService.executeBuy(1L, "AAPL", 500);
        });

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void executeBuy_ShouldProcessTrade_WhenFundsAreSufficient() {
        BigDecimal startingBalance = mockUser.getCashBalance();
        BigDecimal stockPrice = new BigDecimal("50.00");
        int quantity = 2;
        BigDecimal totalCost = stockPrice.multiply(BigDecimal.valueOf(quantity));

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(mockPortfolio));
        when(marketDataProvider.getLivePrice("AAPL")).thenReturn(new StockQuote("AAPL", stockPrice));
        when(holdingRepository.findByPortfolioIdAndSymbol(1L, "AAPL")).thenReturn(Optional.empty());

        transactionManagerService.executeBuy(1L, "AAPL", quantity);

        BigDecimal expectedBalance = startingBalance.subtract(totalCost);
        assertEquals(0, expectedBalance.compareTo(mockUser.getCashBalance()), "Cash was not deducted correctly");

        verify(holdingRepository, times(1)).saveAndFlush(any(Holding.class));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void executeBuy_ShouldCalculateCorrectAveragePrice_WhenBuyingExistingStock() {
        Holding existingHolding = new Holding(mockPortfolio, "AAPL", 10, new BigDecimal("100.00"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(mockPortfolio));
        when(holdingRepository.findByPortfolioIdAndSymbol(1L, "AAPL")).thenReturn(Optional.of(existingHolding));
        when(marketDataProvider.getLivePrice("AAPL")).thenReturn(new StockQuote("AAPL", new BigDecimal("50.00")));

        transactionManagerService.executeBuy(1L, "AAPL", 10);

        assertEquals(20, existingHolding.getQuantity());
        assertEquals(0, new BigDecimal("75.00").compareTo(existingHolding.getAverageBuyPrice()));

        verify(holdingRepository, times(1)).saveAndFlush(existingHolding);
    }

    @Test
    void executeSell_ShouldBlockTrade_WhenSellingUnownedStock() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(mockPortfolio));
        when(holdingRepository.findByPortfolioIdAndSymbol(1L, "TSLA")).thenReturn(Optional.empty());

        assertThrows(InsufficientSharesException.class, () -> {
            transactionManagerService.executeSell(1L, "TSLA", 10);
        });

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void executeSell_ShouldDeleteHolding_WhenSellingAllShares() {
        BigDecimal startingBalance = mockUser.getCashBalance();
        BigDecimal sellPrice = new BigDecimal("250.00");
        int quantity = 5;
        BigDecimal totalRevenue = sellPrice.multiply(BigDecimal.valueOf(quantity));

        Holding existingHolding = new Holding(mockPortfolio, "TSLA", quantity, new BigDecimal("200.00"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(mockPortfolio));
        when(holdingRepository.findByPortfolioIdAndSymbol(1L, "TSLA")).thenReturn(Optional.of(existingHolding));
        when(marketDataProvider.getLivePrice("TSLA")).thenReturn(new StockQuote("TSLA", sellPrice));

        transactionManagerService.executeSell(1L, "TSLA", quantity);

        BigDecimal expectedBalance = startingBalance.add(totalRevenue);
        assertEquals(0, expectedBalance.compareTo(mockUser.getCashBalance()), "Cash was not added correctly");

        verify(holdingRepository, times(1)).delete(existingHolding);
    }

    @Test
    void executeBuy_ShouldThrowException_WhenQuantityIsZeroOrNegative() {
        assertThrows(IllegalArgumentException.class, () -> {
            transactionManagerService.executeBuy(1L, "AAPL", 0);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            transactionManagerService.executeBuy(1L, "AAPL", -5);
        });

        verify(marketDataProvider, never()).getLivePrice(anyString());
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void executeSell_ShouldBlockTrade_WhenSellingMoreSharesThanOwned() {
        Holding existingHolding = new Holding(mockPortfolio, "TSLA", 5, new BigDecimal("200.00"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(mockPortfolio));
        when(holdingRepository.findByPortfolioIdAndSymbol(1L, "TSLA")).thenReturn(Optional.of(existingHolding));
        when(marketDataProvider.getLivePrice("TSLA")).thenReturn(new StockQuote("TSLA", new BigDecimal("250.00")));

        assertThrows(IllegalArgumentException.class, () -> {
            transactionManagerService.executeSell(1L, "TSLA", 6);
        });

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void executeSell_ShouldProcessPartialSell_AndKeepAverageBuyPriceUnchanged() {
        BigDecimal startingBalance = mockUser.getCashBalance();
        BigDecimal sellPrice = new BigDecimal("150.00");
        int sellQuantity = 4;
        BigDecimal expectedRevenue = sellPrice.multiply(BigDecimal.valueOf(sellQuantity));

        Holding existingHolding = new Holding(mockPortfolio, "AAPL", 10, new BigDecimal("100.00"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(mockPortfolio));
        when(holdingRepository.findByPortfolioIdAndSymbol(1L, "AAPL")).thenReturn(Optional.of(existingHolding));
        when(marketDataProvider.getLivePrice("AAPL")).thenReturn(new StockQuote("AAPL", sellPrice));

        transactionManagerService.executeSell(1L, "AAPL", sellQuantity);

        BigDecimal expectedBalance = startingBalance.add(expectedRevenue);
        assertEquals(0, expectedBalance.compareTo(mockUser.getCashBalance()), "Cash was not added correctly after partial sell");

        assertEquals(6, existingHolding.getQuantity(), "Holding quantity should decrease by the sold amount");
        assertEquals(0, new BigDecimal("100.00").compareTo(existingHolding.getAverageBuyPrice()), "Average buy price should NOT change on a sell");

        verify(holdingRepository, times(1)).saveAndFlush(existingHolding);
        verify(holdingRepository, never()).delete(any());
    }
}
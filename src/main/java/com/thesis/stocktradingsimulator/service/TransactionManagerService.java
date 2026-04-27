package com.thesis.stocktradingsimulator.service;

import com.thesis.stocktradingsimulator.exception.InsufficientSharesException;
import com.thesis.stocktradingsimulator.exception.ResourceNotFoundException;
import com.thesis.stocktradingsimulator.model.*;
import com.thesis.stocktradingsimulator.model.Transaction.TransactionType;
import com.thesis.stocktradingsimulator.repository.HoldingRepository;
import com.thesis.stocktradingsimulator.repository.PortfolioRepository;
import com.thesis.stocktradingsimulator.repository.TransactionRepository;
import com.thesis.stocktradingsimulator.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Optional;

@Service
public class TransactionManagerService {
    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    private final TransactionRepository transactionRepository;
    private final MarketDataProvider marketDataProvider;

    public TransactionManagerService(UserRepository userRepository, PortfolioRepository portfolioRepository,
                                     HoldingRepository holdingRepository, TransactionRepository transactionRepository,
                                     MarketDataProvider marketDataProvider) {
        this.userRepository = userRepository;
        this.portfolioRepository = portfolioRepository;
        this.holdingRepository = holdingRepository;
        this.transactionRepository = transactionRepository;
        this.marketDataProvider = marketDataProvider;
    }

    @Transactional
    public Transaction executeBuy(Long userId, String symbol, int quantity) {
        validateQuantity(quantity);

        User user = getUserOrThrow(userId);
        Portfolio portfolio = getPortfolioOrThrow(userId);

        BigDecimal price = getValidStockPrice(symbol);
        BigDecimal totalCost = calculateTotalValue(price, quantity);

        user.deductFunds(totalCost);
        userRepository.save(user);

        Optional<Holding> holdingOption = holdingRepository.findByPortfolioIdAndSymbol(portfolio.getId(), symbol.toUpperCase());
        if(holdingOption.isPresent()){
            Holding holding = holdingOption.get();
            holding.addShares(quantity, price);
            holdingRepository.save(holding);
        } else {
            Holding newHolding = new Holding(portfolio, symbol.toUpperCase(), quantity, price);
            holdingRepository.save(newHolding);
        }

        Transaction transaction = new Transaction(portfolio, TransactionType.BUY, symbol.toUpperCase(), quantity, price);
        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction executeSell(Long userId, String symbol, int quantity) {
        validateQuantity(quantity);

        User user = getUserOrThrow(userId);
        Portfolio portfolio = getPortfolioOrThrow(userId);

        Holding holding = holdingRepository.findByPortfolioIdAndSymbol(portfolio.getId(), symbol.toUpperCase())
                .orElseThrow(() -> new InsufficientSharesException("You do not own any shares of " + symbol.toUpperCase()));

        BigDecimal price = getValidStockPrice(symbol);
        BigDecimal totalRevenue = calculateTotalValue(price, quantity);

        user.addFunds(totalRevenue);
        userRepository.save(user);

        holding.removeShares(quantity);

        if (holding.getQuantity() == 0) {
            holdingRepository.delete(holding);
        } else {
            holdingRepository.save(holding);
        }

        Transaction transaction = new Transaction(portfolio, TransactionType.SELL, symbol.toUpperCase(), quantity, price);
        return transactionRepository.save(transaction);
    }
    public java.util.List<Transaction> getTransactionHistory(Long userId) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId).orElse(null);
        if (portfolio == null) {
            return Collections.emptyList();
        }
        return transactionRepository.findByPortfolioIdOrderByTimestampDesc(portfolio.getId());
    }

    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    private Portfolio getPortfolioOrThrow(Long userId) {
        return portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found."));
    }

    private BigDecimal getValidStockPrice(String symbol) {
        StockQuote quote = marketDataProvider.getLivePrice(symbol);
        if (quote == null) {
            throw new ResourceNotFoundException("Invalid stock symbol or API error.");
        }
        return quote.getCurrentPrice();
    }

    private BigDecimal calculateTotalValue(BigDecimal price, int quantity) {
        return price.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }
}
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
import java.util.Optional;

@Service
public class TransactionManagerService {
    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    private final TransactionRepository transactionRepository;
    private final MarketDataService marketDataService;

    public TransactionManagerService(UserRepository userRepository, PortfolioRepository portfolioRepository,
                                     HoldingRepository holdingRepository, TransactionRepository transactionRepository,
                                     MarketDataService marketDataService) {
        this.userRepository = userRepository;
        this.portfolioRepository = portfolioRepository;
        this.holdingRepository = holdingRepository;
        this.transactionRepository = transactionRepository;
        this.marketDataService = marketDataService;
    }

    @Transactional
    public Transaction executeBuy(Long userId, String symbol, int quantity) {
        validateQuantity(quantity);

        User user = getUserOrThrow(userId);
        Portfolio portfolio = getPortfolioOrThrow(userId);

        BigDecimal price = getValidStockPrice(symbol);
        BigDecimal totalCost = calculateTotalValue(price, quantity);

        user.deductFunds(totalCost);
        userRepository.saveAndFlush(user);

        Optional<Holding> holdingOption = holdingRepository.findByPortfolioIdAndSymbol(portfolio.getId(), symbol.toUpperCase());
        if(holdingOption.isPresent()){
            Holding holding = holdingOption.get();
            BigDecimal prevQty = BigDecimal.valueOf(holding.getQuantity());
            BigDecimal totalSpentPreviously = prevQty.multiply(holding.getAverageBuyPrice());
            BigDecimal newTotalSpent = totalSpentPreviously.add(totalCost);

            int newQuantity = holding.getQuantity() + quantity;
            BigDecimal newAveragePrice = newTotalSpent.divide(BigDecimal.valueOf(newQuantity), 2, RoundingMode.HALF_UP);

            holding.setQuantity(newQuantity);
            holding.setAverageBuyPrice(newAveragePrice);
            holdingRepository.saveAndFlush(holding);
        } else {
            Holding newHolding = new Holding(portfolio, symbol.toUpperCase(), quantity, price);
            holdingRepository.saveAndFlush(newHolding);
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

        if (holding.getQuantity() < quantity) {
            throw new InsufficientSharesException("Insufficient shares. You only have " + holding.getQuantity());
        }

        BigDecimal price = getValidStockPrice(symbol);
        BigDecimal totalRevenue = calculateTotalValue(price, quantity);

        user.addFunds(totalRevenue);
        userRepository.saveAndFlush(user);

        int remainingQuantity = holding.getQuantity() - quantity;
        if (remainingQuantity == 0) {
            holdingRepository.delete(holding);
        } else {
            holding.setQuantity(remainingQuantity);
            holdingRepository.saveAndFlush(holding);
        }

        Transaction transaction = new Transaction(portfolio, TransactionType.SELL, symbol.toUpperCase(), quantity, price);
        return transactionRepository.save(transaction);
    }

    public java.util.List<Transaction> getTransactionHistory(Long userId) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId).orElse(null);
        if (portfolio == null) {
            return java.util.Collections.emptyList();
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
        StockQuote quote = marketDataService.getLivePrice(symbol);
        if (quote == null) {
            throw new ResourceNotFoundException("Invalid stock symbol or API error.");
        }
        return quote.getCurrentPrice();
    }

    private BigDecimal calculateTotalValue(BigDecimal price, int quantity) {
        return price.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }
}
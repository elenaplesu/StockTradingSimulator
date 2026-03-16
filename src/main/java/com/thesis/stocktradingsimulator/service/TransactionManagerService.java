package com.thesis.stocktradingsimulator.service;

import com.thesis.stocktradingsimulator.model.*;
import com.thesis.stocktradingsimulator.repository.HoldingRepository;
import com.thesis.stocktradingsimulator.repository.PortfolioRepository;
import com.thesis.stocktradingsimulator.repository.TransactionRepository;
import com.thesis.stocktradingsimulator.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public String executeBuy(Long userId, String symbol, int quantity) {
        if (quantity <= 0) return "Error: Quantity must be greater than zero.";

        User user = userRepository.findById(userId).orElse(null);
        Portfolio portfolio = portfolioRepository.findByUserId(userId).orElse(null);
        if (user == null || portfolio == null) return "Error: User or Portfolio not found.";


        StockQuote quote = marketDataService.getLivePrice(symbol);
        if (quote == null) return "Error: Invalid stock symbol or API error.";
        double price = quote.getCurrentPrice();
        double totalCost = price * quantity;
        double totalCostRounded=Math.round(totalCost * 100.0) / 100.0;

        if (user.getCashBalance() < totalCostRounded) {
            return "Error: Insufficient funds. Need $" + totalCostRounded + " but have $" + user.getCashBalance();
        }

        user.setCashBalance(user.getCashBalance() - totalCostRounded);
        userRepository.save(user);

        Optional<Holding> holdingOption = holdingRepository.findByPortfolioIdAndSymbol(portfolio.getId(), symbol.toUpperCase());
        if(holdingOption.isPresent()){
            Holding holding = holdingOption.get();
            // Calculate new average buy price
            double totalSpentPreviously = holding.getQuantity() * holding.getAverageBuyPrice();
            double newTotalSpent = totalSpentPreviously + totalCostRounded;
            int newQuantity = holding.getQuantity() + quantity;

            holding.setQuantity(newQuantity);
            double newAveragePrice = newTotalSpent / newQuantity;
            holding.setAverageBuyPrice(Math.round(newAveragePrice * 100.0) / 100.0);
            holdingRepository.save(holding);
        }else {
            // Create a brand new holding
            Holding newHolding = new Holding(portfolio, symbol.toUpperCase(), quantity, price);
            holdingRepository.save(newHolding);
        }

        Transaction transaction = new Transaction(portfolio, "BUY", symbol.toUpperCase(), quantity, price);
        transactionRepository.save(transaction);

        return "Success: Bought " + quantity + " shares of " + symbol.toUpperCase() + " for $" + String.format("%.2f", totalCostRounded);
    }
    @Transactional
    public String executeSell(Long userId, String symbol, int quantity) {
        if (quantity <= 0) return "Error: Quantity must be greater than zero.";
        //note to remove code repetition later
        User user = userRepository.findById(userId).orElse(null);
        Portfolio portfolio = portfolioRepository.findByUserId(userId).orElse(null);
        if (user == null || portfolio == null) return "Error: User or Portfolio not found.";

        Optional<Holding> holdingOption = holdingRepository.findByPortfolioIdAndSymbol(portfolio.getId(), symbol.toUpperCase());
        if (holdingOption.isEmpty()) {
            return "Error: You do not own any shares of " + symbol.toUpperCase();
        }

        Holding holding = holdingOption.get();
        if (holding.getQuantity() < quantity) {
            return "Error: Insufficient shares. You only have " + holding.getQuantity() + " shares.";
        }

        StockQuote quote = marketDataService.getLivePrice(symbol);
        if (quote == null) return "Error: Invalid stock symbol or API error.";

        double price = quote.getCurrentPrice();
        double totalRevenue = price * quantity;
        double totalRevenueRounded = Math.round(totalRevenue * 100.0) / 100.0;

        user.setCashBalance(user.getCashBalance() + totalRevenueRounded);
        userRepository.save(user);


        int remainingQuantity = holding.getQuantity() - quantity;
        if (remainingQuantity == 0) {
            // If they sold everything, remove the holding entirely
            holdingRepository.delete(holding);
        } else {
            // Otherwise, just update (average buy price stays the same)
            holding.setQuantity(remainingQuantity);
            holdingRepository.save(holding);
        }


        Transaction transaction = new Transaction(portfolio, "SELL", symbol.toUpperCase(), quantity, price);
        transactionRepository.save(transaction);

        return "Success: Sold " + quantity + " shares of " + symbol.toUpperCase() + " for $" + String.format("%.2f", totalRevenueRounded);
    }

    public java.util.List<Transaction> getTransactionHistory(Long userId) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId).orElse(null);
        if (portfolio == null) {
            return java.util.Collections.emptyList();
        }
        return transactionRepository.findByPortfolioIdOrderByTimestampDesc(portfolio.getId());
    }

}

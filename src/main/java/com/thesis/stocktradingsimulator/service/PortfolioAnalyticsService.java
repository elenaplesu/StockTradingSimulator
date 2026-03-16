package com.thesis.stocktradingsimulator.service;

import com.thesis.stocktradingsimulator.dto.EnrichedHoldingDTO;
import com.thesis.stocktradingsimulator.dto.PortfolioAnalyticsDTO;
import com.thesis.stocktradingsimulator.model.Holding;
import com.thesis.stocktradingsimulator.model.Portfolio;
import com.thesis.stocktradingsimulator.model.User;
import com.thesis.stocktradingsimulator.repository.HoldingRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PortfolioAnalyticsService {

    private final UserService userService;
    private final HoldingRepository holdingRepository;
    private final RestTemplate restTemplate;

    private final String FINNHUB_KEY = "d6krbspr01qmopd1r680d6krbspr01qmopd1r68g";

    public PortfolioAnalyticsService(UserService userService, HoldingRepository holdingRepository) {
        this.userService = userService;
        this.holdingRepository = holdingRepository;
        this.restTemplate = new RestTemplate();
    }

    private double fetchLivePrice(String symbol) {
        try {
            String url = String.format("https://finnhub.io/api/v1/quote?symbol=%s&token=%s", symbol.toUpperCase(), FINNHUB_KEY);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.get("c") != null) {
                return Double.parseDouble(response.get("c").toString());
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch price for " + symbol + ": " + e.getMessage());
        }
        return 0.0;
    }

    public PortfolioAnalyticsDTO generateAnalytics(Long userId) {
        User user = userService.getUserById(userId);
        Portfolio portfolio = userService.getPortfolioByUserId(userId);
        List<Holding> rawHoldings = holdingRepository.findByPortfolioId(portfolio.getId());

        if (rawHoldings.isEmpty()) {
            return new PortfolioAnalyticsDTO(user.getCashBalance(), 0.0, user.getCashBalance(), 0.0, 0.0, 0.0, new ArrayList<>());
        }

        double totalEquity = 0.0;
        double totalCostBasis = 0.0;
        List<Double> individualReturns = new ArrayList<>();
        List<EnrichedHoldingDTO> tempHoldings = new ArrayList<>();


        for (Holding holding : rawHoldings) {
            double livePrice = fetchLivePrice(holding.getSymbol());

            if (livePrice == 0.0) livePrice = holding.getAverageBuyPrice();

            double currentHoldingValue = holding.getQuantity() * livePrice;
            double costBasis = holding.getQuantity() * holding.getAverageBuyPrice();

            totalEquity += currentHoldingValue;
            totalCostBasis += costBasis;

            // R_i = (P_i - C_i) / C_i
            double holdingROI = ((livePrice - holding.getAverageBuyPrice()) / holding.getAverageBuyPrice()) * 100;
            individualReturns.add(holdingROI);

            // Temporarily store with 0.0 weight
            tempHoldings.add(new EnrichedHoldingDTO(
                    holding.getSymbol(), holding.getQuantity(), holding.getAverageBuyPrice(),
                    livePrice, currentHoldingValue, 0.0, holdingROI
            ));
        }

        double hhi = 0.0;
        List<EnrichedHoldingDTO> finalizedHoldings = new ArrayList<>();

        // Calculate Asset Weights (W_i) and HHI
        for (EnrichedHoldingDTO dto : tempHoldings) {
            double weight = dto.getTotalValue() / totalEquity;
            hhi += Math.pow(weight, 2);

            // Rebuild DTO with the correct weight percentage
            finalizedHoldings.add(new EnrichedHoldingDTO(
                    dto.getSymbol(), dto.getQuantity(), dto.getAverageBuyPrice(),
                    dto.getCurrentPrice(), dto.getTotalValue(), weight * 100, dto.getReturnOnInvestment()
            ));
        }

        double normalizedHHI = hhi * 10000;

        // Calculate Aggregate ROI
        double aggregateROI = ((totalEquity - totalCostBasis) / totalCostBasis) * 100;

        // Calculate Cross-Sectional Variance (Sigma Squared)
        double sumReturns = 0.0;
        for (double r : individualReturns) { sumReturns += r; }
        double meanReturn = sumReturns / individualReturns.size();

        double varianceSum = 0.0;
        for (double r : individualReturns) {
            varianceSum += Math.pow(r - meanReturn, 2);
        }
        double crossSectionalVariance = varianceSum / individualReturns.size();

        double netWorth = user.getCashBalance() + totalEquity;

        return new PortfolioAnalyticsDTO(
                user.getCashBalance(),
                totalEquity,
                netWorth,
                aggregateROI,
                normalizedHHI,
                crossSectionalVariance,
                finalizedHoldings
        );
    }
}
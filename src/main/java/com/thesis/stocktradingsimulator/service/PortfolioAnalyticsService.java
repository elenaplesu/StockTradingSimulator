package com.thesis.stocktradingsimulator.service;

import com.thesis.stocktradingsimulator.dto.EnrichedHoldingDTO;
import com.thesis.stocktradingsimulator.dto.PortfolioAnalyticsDTO;
import com.thesis.stocktradingsimulator.model.Holding;
import com.thesis.stocktradingsimulator.model.Portfolio;
import com.thesis.stocktradingsimulator.model.StockQuote;
import com.thesis.stocktradingsimulator.model.User;
import com.thesis.stocktradingsimulator.repository.HoldingRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class PortfolioAnalyticsService {

    private final UserService userService;
    private final HoldingRepository holdingRepository;
    private final MarketDataService marketDataService;

    public PortfolioAnalyticsService(UserService userService, HoldingRepository holdingRepository, MarketDataService marketDataService) {
        this.userService = userService;
        this.holdingRepository = holdingRepository;
        this.marketDataService = marketDataService;
    }

    public PortfolioAnalyticsDTO generateAnalytics(Long userId) {
        User user = userService.getUserById(userId);
        Portfolio portfolio = userService.getPortfolioByUserId(userId);
        List<Holding> rawHoldings = holdingRepository.findByPortfolioId(portfolio.getId());

        if (rawHoldings.isEmpty()) {
            return new PortfolioAnalyticsDTO(
                    user.getCashBalance(), BigDecimal.ZERO, user.getCashBalance(),
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new ArrayList<>()
            );
        }

        BigDecimal totalEquity = BigDecimal.ZERO;
        BigDecimal totalCostBasis = BigDecimal.ZERO;
        List<BigDecimal> individualReturns = new ArrayList<>();
        List<EnrichedHoldingDTO> tempHoldings = new ArrayList<>();

        for (Holding holding : rawHoldings) {
            StockQuote quote = marketDataService.getLivePrice(holding.getSymbol());
            BigDecimal livePrice = (quote != null && quote.getCurrentPrice() != null)
                    ? quote.getCurrentPrice()
                    : holding.getAverageBuyPrice(); // Fallback to avg buy price if API fails

            BigDecimal quantity = BigDecimal.valueOf(holding.getQuantity());

            BigDecimal currentHoldingValue = quantity.multiply(livePrice).setScale(2, RoundingMode.HALF_UP);
            BigDecimal costBasis = quantity.multiply(holding.getAverageBuyPrice()).setScale(2, RoundingMode.HALF_UP);

            totalEquity = totalEquity.add(currentHoldingValue);
            totalCostBasis = totalCostBasis.add(costBasis);

            BigDecimal holdingROI = BigDecimal.ZERO;
            if (holding.getAverageBuyPrice().compareTo(BigDecimal.ZERO) > 0) {
                holdingROI = livePrice.subtract(holding.getAverageBuyPrice())
                        .divide(holding.getAverageBuyPrice(), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .setScale(2, RoundingMode.HALF_UP);
            }

            individualReturns.add(holdingROI);

            // Temporarily store with 0.0 weight
            tempHoldings.add(new EnrichedHoldingDTO(
                    holding.getSymbol(), holding.getQuantity(), holding.getAverageBuyPrice(),
                    livePrice, currentHoldingValue, BigDecimal.ZERO, holdingROI
            ));
        }

        BigDecimal hhi = BigDecimal.ZERO;
        List<EnrichedHoldingDTO> finalizedHoldings = new ArrayList<>();

        // Calculate Asset Weights (W_i) and HHI
        for (EnrichedHoldingDTO dto : tempHoldings) {
            BigDecimal weight = BigDecimal.ZERO;
            if (totalEquity.compareTo(BigDecimal.ZERO) > 0) {
                weight = dto.totalValue().divide(totalEquity, 4, RoundingMode.HALF_UP);
            }

            hhi = hhi.add(weight.pow(2));

            BigDecimal weightPercentage = weight.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);

            finalizedHoldings.add(new EnrichedHoldingDTO(
                    dto.symbol(), dto.quantity(), dto.averageBuyPrice(),
                    dto.currentPrice(), dto.totalValue(), weightPercentage, dto.returnOnInvestment()
            ));
        }

        BigDecimal normalizedHHI = hhi.multiply(new BigDecimal("10000")).setScale(2, RoundingMode.HALF_UP);

        BigDecimal aggregateROI = BigDecimal.ZERO;
        if (totalCostBasis.compareTo(BigDecimal.ZERO) > 0) {
            aggregateROI = totalEquity.subtract(totalCostBasis)
                    .divide(totalCostBasis, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal sumReturns = BigDecimal.ZERO;
        for (BigDecimal r : individualReturns) {
            sumReturns = sumReturns.add(r);
        }

        BigDecimal count = BigDecimal.valueOf(individualReturns.size());
        BigDecimal meanReturn = sumReturns.divide(count, 4, RoundingMode.HALF_UP);

        BigDecimal varianceSum = BigDecimal.ZERO;
        for (BigDecimal r : individualReturns) {
            BigDecimal difference = r.subtract(meanReturn);
            varianceSum = varianceSum.add(difference.pow(2));
        }
        BigDecimal crossSectionalVariance = varianceSum.divide(count, 4, RoundingMode.HALF_UP);

        BigDecimal netWorth = user.getCashBalance().add(totalEquity);

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
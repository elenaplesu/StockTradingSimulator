package com.thesis.stocktradingsimulator.service;

import com.thesis.stocktradingsimulator.dto.EnrichedHoldingDTO;
import com.thesis.stocktradingsimulator.dto.PortfolioAnalyticsDTO;
import com.thesis.stocktradingsimulator.model.Holding;
import com.thesis.stocktradingsimulator.model.Portfolio;
import com.thesis.stocktradingsimulator.model.StockQuote;
import com.thesis.stocktradingsimulator.model.User;
import com.thesis.stocktradingsimulator.repository.HoldingRepository;
import com.thesis.stocktradingsimulator.util.PortfolioMathUtils; // <-- Add this
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class PortfolioAnalyticsService {

    private final UserService userService;
    private final HoldingRepository holdingRepository;
    private final MarketDataProvider marketDataProvider; // Assuming you did the 3.A fix!

    public PortfolioAnalyticsService(UserService userService, HoldingRepository holdingRepository, MarketDataProvider marketDataProvider) {
        this.userService = userService;
        this.holdingRepository = holdingRepository;
        this.marketDataProvider = marketDataProvider;
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
            StockQuote quote = marketDataProvider.getLivePrice(holding.getSymbol());
            BigDecimal livePrice = (quote != null && quote.getCurrentPrice() != null)
                    ? quote.getCurrentPrice() : holding.getAverageBuyPrice();

            BigDecimal quantity = BigDecimal.valueOf(holding.getQuantity());
            BigDecimal currentHoldingValue = quantity.multiply(livePrice).setScale(2, RoundingMode.HALF_UP);
            BigDecimal costBasis = quantity.multiply(holding.getAverageBuyPrice()).setScale(2, RoundingMode.HALF_UP);

            totalEquity = totalEquity.add(currentHoldingValue);
            totalCostBasis = totalCostBasis.add(costBasis);

            BigDecimal holdingROI = PortfolioMathUtils.calculateROI(livePrice, holding.getAverageBuyPrice());
            individualReturns.add(holdingROI);

            tempHoldings.add(new EnrichedHoldingDTO(
                    holding.getSymbol(), holding.getQuantity(), holding.getAverageBuyPrice(),
                    livePrice, currentHoldingValue, BigDecimal.ZERO, holdingROI
            ));
        }

        List<BigDecimal> weights = new ArrayList<>();
        List<EnrichedHoldingDTO> finalizedHoldings = new ArrayList<>();

        for (EnrichedHoldingDTO dto : tempHoldings) {
            BigDecimal weight = BigDecimal.ZERO;
            if (totalEquity.compareTo(BigDecimal.ZERO) > 0) {
                weight = dto.totalValue().divide(totalEquity, 4, RoundingMode.HALF_UP);
            }
            weights.add(weight);

            BigDecimal weightPercentage = weight.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
            finalizedHoldings.add(new EnrichedHoldingDTO(
                    dto.symbol(), dto.quantity(), dto.averageBuyPrice(),
                    dto.currentPrice(), dto.totalValue(), weightPercentage, dto.returnOnInvestment()
            ));
        }

        BigDecimal normalizedHHI = PortfolioMathUtils.calculateNormalizedHHI(weights);
        BigDecimal aggregateROI = PortfolioMathUtils.calculateROI(totalEquity, totalCostBasis);
        BigDecimal crossSectionalVariance = PortfolioMathUtils.calculateCrossSectionalVariance(individualReturns);
        BigDecimal netWorth = user.getCashBalance().add(totalEquity);

        return new PortfolioAnalyticsDTO(
                user.getCashBalance(), totalEquity, netWorth, aggregateROI,
                normalizedHHI, crossSectionalVariance, finalizedHoldings
        );
    }
}
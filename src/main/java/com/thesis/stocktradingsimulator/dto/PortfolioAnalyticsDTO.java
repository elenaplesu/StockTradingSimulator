package com.thesis.stocktradingsimulator.dto;

import java.util.List;

public class PortfolioAnalyticsDTO {
    private double cashBalance;
    private double totalEquity;
    private double netWorth;

    // The Macro Math Fields
    private double aggregateROI;
    private double hhi;
    private double crossSectionalVariance;

    private List<EnrichedHoldingDTO> holdings;

    public PortfolioAnalyticsDTO(double cashBalance, double totalEquity, double netWorth, double aggregateROI, double hhi, double crossSectionalVariance, List<EnrichedHoldingDTO> holdings) {
        this.cashBalance = cashBalance;
        this.totalEquity = totalEquity;
        this.netWorth = netWorth;
        this.aggregateROI = aggregateROI;
        this.hhi = hhi;
        this.crossSectionalVariance = crossSectionalVariance;
        this.holdings = holdings;
    }

    // Getters
    public double getCashBalance() { return cashBalance; }
    public double getTotalEquity() { return totalEquity; }
    public double getNetWorth() { return netWorth; }
    public double getAggregateROI() { return aggregateROI; }
    public double getHhi() { return hhi; }
    public double getCrossSectionalVariance() { return crossSectionalVariance; }
    public List<EnrichedHoldingDTO> getHoldings() { return holdings; }
}
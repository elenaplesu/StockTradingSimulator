package com.thesis.stocktradingsimulator.repository;

import com.thesis.stocktradingsimulator.model.Holding;
import com.thesis.stocktradingsimulator.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HoldingRepository extends JpaRepository<Holding, Long> {
    // Find a specific stock in a specific portfolio
    Optional<Holding> findByPortfolioIdAndSymbol(Long portfolioId, String symbol);

    // Get all holdings for a portfolio
    List<Holding> findByPortfolioId(Long portfolioId);
}

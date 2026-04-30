package com.thesis.stocktradingsimulator.repository;

import com.thesis.stocktradingsimulator.model.Holding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HoldingRepository extends JpaRepository<Holding, Long> {
    Optional<Holding> findByPortfolioIdAndSymbol(Long portfolioId, String symbol);

    List<Holding> findByPortfolioId(Long portfolioId);
}

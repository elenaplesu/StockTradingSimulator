package com.thesis.stocktradingsimulator.repository;

import com.thesis.stocktradingsimulator.model.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long>{
    // Find the portfolio that belongs to a specific user
    Optional<Portfolio> findByUserId(Long userId);
}

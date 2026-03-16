package com.thesis.stocktradingsimulator.controller;

import com.thesis.stocktradingsimulator.model.Portfolio;
import com.thesis.stocktradingsimulator.model.User;
import com.thesis.stocktradingsimulator.repository.HoldingRepository;
import com.thesis.stocktradingsimulator.service.UserService;
import com.thesis.stocktradingsimulator.service.TransactionManagerService; // 1. Added Import
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.thesis.stocktradingsimulator.dto.PortfolioAnalyticsDTO;
import com.thesis.stocktradingsimulator.service.PortfolioAnalyticsService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/portfolio")
@CrossOrigin(origins = "http://localhost:5173")
public class PortfolioController {

    private final UserService userService;
    private final HoldingRepository holdingRepository;
    private final PortfolioAnalyticsService analyticsService;
    private final TransactionManagerService transactionManagerService; // 2. Declared the service

    public PortfolioController(UserService userService,
                               HoldingRepository holdingRepository,
                               PortfolioAnalyticsService analyticsService,
                               TransactionManagerService transactionManagerService) {
        this.userService = userService;
        this.holdingRepository = holdingRepository;
        this.analyticsService = analyticsService;
        this.transactionManagerService = transactionManagerService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getPortfolioData(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        Portfolio portfolio = userService.getPortfolioByUserId(userId);

        if (user == null || portfolio == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("cashBalance", user.getCashBalance());
        response.put("holdings", holdingRepository.findByPortfolioId(portfolio.getId()));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/analytics")
    public ResponseEntity<PortfolioAnalyticsDTO> getPortfolioAnalytics(@PathVariable Long userId) {
        try {
            PortfolioAnalyticsDTO analytics = analyticsService.generateAnalytics(userId);
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            System.err.println("Error generating analytics: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{userId}/transactions")
    public ResponseEntity<?> getTransactionHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(transactionManagerService.getTransactionHistory(userId));
    }
}
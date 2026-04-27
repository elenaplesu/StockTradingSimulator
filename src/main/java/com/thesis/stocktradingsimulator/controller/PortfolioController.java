package com.thesis.stocktradingsimulator.controller;

import com.thesis.stocktradingsimulator.dto.HoldingDTO;
import com.thesis.stocktradingsimulator.dto.PortfolioAnalyticsDTO;
import com.thesis.stocktradingsimulator.model.Portfolio;
import com.thesis.stocktradingsimulator.model.User;
import com.thesis.stocktradingsimulator.repository.HoldingRepository;
import com.thesis.stocktradingsimulator.repository.UserRepository;
import com.thesis.stocktradingsimulator.service.PortfolioAnalyticsService;
import com.thesis.stocktradingsimulator.service.TransactionManagerService;
import com.thesis.stocktradingsimulator.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final HoldingRepository holdingRepository;
    private final PortfolioAnalyticsService analyticsService;
    private final TransactionManagerService transactionManagerService;

    public PortfolioController(UserService userService,
                               UserRepository userRepository,
                               HoldingRepository holdingRepository,
                               PortfolioAnalyticsService analyticsService,
                               TransactionManagerService transactionManagerService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.holdingRepository = holdingRepository;
        this.analyticsService = analyticsService;
        this.transactionManagerService = transactionManagerService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getPortfolioData(@PathVariable Long userId, Authentication authentication) {
        if (isUnauthorized(userId, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        User user = userService.getUserById(userId);
        Portfolio portfolio = userService.getPortfolioByUserId(userId);

        if (user == null || portfolio == null) {
            return ResponseEntity.notFound().build();
        }

        List<HoldingDTO> holdingDTOs = holdingRepository.findByPortfolioId(portfolio.getId())
                .stream()
                .map(h -> new HoldingDTO(h.getSymbol(), h.getQuantity(), h.getAverageBuyPrice()))
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("cashBalance", user.getCashBalance());
        response.put("holdings", holdingDTOs);

        return ResponseEntity.ok(response);
    }
    @GetMapping("/{userId}/analytics")
    public ResponseEntity<PortfolioAnalyticsDTO> getPortfolioAnalytics(@PathVariable Long userId, Authentication authentication) {
        if (isUnauthorized(userId, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(analyticsService.generateAnalytics(userId));
    }

    @GetMapping("/{userId}/transactions")
    public ResponseEntity<?> getTransactionHistory(@PathVariable Long userId, Authentication authentication) {
        if (isUnauthorized(userId, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(transactionManagerService.getTransactionHistory(userId));
    }

    private boolean isUnauthorized(Long userId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return true;
        User caller = userRepository.findByUsername(authentication.getName()).orElse(null);
        return caller == null || !caller.getId().equals(userId);
    }
}
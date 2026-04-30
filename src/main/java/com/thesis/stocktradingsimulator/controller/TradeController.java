package com.thesis.stocktradingsimulator.controller;

import com.thesis.stocktradingsimulator.model.Transaction;
import com.thesis.stocktradingsimulator.model.User;
import com.thesis.stocktradingsimulator.repository.UserRepository;
import com.thesis.stocktradingsimulator.service.TransactionManagerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/trade")
public class TradeController {
    private final TransactionManagerService transactionManager;
    private final UserRepository userRepository;

    public TradeController(TransactionManagerService transactionManager, UserRepository userRepository) {
        this.transactionManager = transactionManager;
        this.userRepository = userRepository;
    }

    public record TradeRequest(
            @NotNull Long userId,
            @NotBlank String symbol,
            @Min(1) int quantity
    ) {}

    @PostMapping("/buy")
    public ResponseEntity<?> buyStock(@RequestBody @Valid TradeRequest request, Authentication authentication) {
        if (isUnauthorized(request.userId(), authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Transaction tx = transactionManager.executeBuy(request.userId(), request.symbol(), request.quantity());
        BigDecimal totalCost = tx.getExecutionPrice().multiply(BigDecimal.valueOf(tx.getQuantity()));

        return ResponseEntity.ok(Map.of("message", String.format("Success: Bought %d shares of %s for $%.2f",
                tx.getQuantity(), tx.getSymbol(), totalCost)));
    }

    @PostMapping("/sell")
    public ResponseEntity<?> sellStock(@RequestBody @Valid TradeRequest request, Authentication authentication) {
        if (isUnauthorized(request.userId(), authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Transaction tx = transactionManager.executeSell(request.userId(), request.symbol(), request.quantity());
        BigDecimal totalRevenue = tx.getExecutionPrice().multiply(BigDecimal.valueOf(tx.getQuantity()));

        return ResponseEntity.ok(Map.of("message", String.format("Success: Sold %d shares of %s for $%.2f",
                tx.getQuantity(), tx.getSymbol(), totalRevenue)));
    }

    private boolean isUnauthorized(Long userId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return true;
        User caller = userRepository.findByUsername(authentication.getName()).orElse(null);
        return caller == null || !caller.getId().equals(userId);
    }
}
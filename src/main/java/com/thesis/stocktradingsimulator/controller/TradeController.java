package com.thesis.stocktradingsimulator.controller;

import com.thesis.stocktradingsimulator.model.Transaction;
import com.thesis.stocktradingsimulator.service.TransactionManagerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/trade")
public class TradeController {
    private final TransactionManagerService transactionManager;

    public TradeController(TransactionManagerService transactionManager) {
        this.transactionManager = transactionManager;
    }

    public static class TradeRequest {
        public Long userId;
        public String symbol;
        public int quantity;
    }

    @PostMapping("/buy")
    public ResponseEntity<Map<String, String>> buyStock(@RequestBody TradeRequest request) {

        Transaction tx = transactionManager.executeBuy(request.userId, request.symbol, request.quantity);

        BigDecimal totalCost = tx.getExecutionPrice().multiply(BigDecimal.valueOf(tx.getQuantity()));
        String successMessage = String.format("Success: Bought %d shares of %s for $%.2f",
                tx.getQuantity(), tx.getSymbol(), totalCost);

        return ResponseEntity.ok(Map.of("message", successMessage));
    }

    @PostMapping("/sell")
    public ResponseEntity<Map<String, String>> sellStock(@RequestBody TradeRequest request) {
        Transaction tx = transactionManager.executeSell(request.userId, request.symbol, request.quantity);

        BigDecimal totalRevenue = tx.getExecutionPrice().multiply(BigDecimal.valueOf(tx.getQuantity()));
        String successMessage = String.format("Success: Sold %d shares of %s for $%.2f",
                tx.getQuantity(), tx.getSymbol(), totalRevenue);

        return ResponseEntity.ok(Map.of("message", successMessage));
    }
}
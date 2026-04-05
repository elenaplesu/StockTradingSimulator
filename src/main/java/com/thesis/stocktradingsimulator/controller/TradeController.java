package com.thesis.stocktradingsimulator.controller;

import com.thesis.stocktradingsimulator.service.TransactionManagerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<String> buyStock(@RequestBody TradeRequest request) {
        String result = transactionManager.executeBuy(request.userId, request.symbol, request.quantity);

        if (result.startsWith("Success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
    @PostMapping("/sell")
    public ResponseEntity<String> sellStock(@RequestBody TradeRequest request) {
        String result = transactionManager.executeSell(request.userId, request.symbol, request.quantity);

        if (result.startsWith("Success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
}

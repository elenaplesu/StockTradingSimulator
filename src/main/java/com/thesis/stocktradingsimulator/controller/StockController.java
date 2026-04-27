package com.thesis.stocktradingsimulator.controller;

import com.thesis.stocktradingsimulator.dto.ChartPoint;
import com.thesis.stocktradingsimulator.model.StockQuote;
import com.thesis.stocktradingsimulator.service.MarketDataProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private final MarketDataProvider marketDataProvider;
    private static final Set<String> VALID_RANGES = Set.of("1D", "5D");

    public StockController(MarketDataProvider marketDataProvider) {
        this.marketDataProvider = marketDataProvider;
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<?> getStockPrice(@PathVariable String symbol) {
        StockQuote quote = marketDataProvider.getLivePrice(symbol);
        String companyName = marketDataProvider.getCompanyName(symbol);
        if (quote != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("symbol", quote.getSymbol());
            response.put("currentPrice", quote.getCurrentPrice());
            response.put("companyName", companyName != null ? companyName : quote.getSymbol());

            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Stock symbol not found."));
        }
    }

    @GetMapping("/{symbol}/history")
    public ResponseEntity<?> getStockHistory(
            @PathVariable String symbol,
            @RequestParam(required = false, defaultValue = "1D") String range) {

        if (!VALID_RANGES.contains(range)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid range. Allowed: " + VALID_RANGES));
        }

        List<ChartPoint> history = marketDataProvider.getStockHistory(symbol, range);

        if (history != null && !history.isEmpty()) {
            return ResponseEntity.ok(history);
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "No historical data available."));
        }
    }
}
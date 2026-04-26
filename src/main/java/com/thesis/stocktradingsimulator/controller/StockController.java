package com.thesis.stocktradingsimulator.controller;

import com.thesis.stocktradingsimulator.dto.ChartPoint;
import com.thesis.stocktradingsimulator.model.StockQuote;
import com.thesis.stocktradingsimulator.service.MarketDataProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private final MarketDataProvider marketDataProvider;

    public StockController(MarketDataProvider marketDataProvider) {
        this.marketDataProvider = marketDataProvider;
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<?> getStockPrice(@PathVariable String symbol) {
        StockQuote quote = marketDataProvider.getLivePrice(symbol);

        if (quote != null) {
            return ResponseEntity.ok(quote);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Stock symbol not found."));
        }
    }

    @GetMapping("/{symbol}/history")
    public ResponseEntity<?> getStockHistory(
            @PathVariable String symbol,
            @RequestParam(required = false, defaultValue = "1D") String range) {

        List<ChartPoint> history = marketDataProvider.getStockHistory(symbol, range);

        if (history != null && !history.isEmpty()) {
            return ResponseEntity.ok(history);
        } else {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "No historical data available."));
        }
    }
}
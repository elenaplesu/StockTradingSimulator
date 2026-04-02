package com.thesis.stocktradingsimulator.controller;

import com.thesis.stocktradingsimulator.model.StockQuote;
import com.thesis.stocktradingsimulator.dto.ChartPoint;
import com.thesis.stocktradingsimulator.service.MarketDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/stocks")
public class StockController {

    private final MarketDataService marketDataService;

    public StockController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<StockQuote> getStockPrice(@PathVariable String symbol) {
        StockQuote quote = marketDataService.getLivePrice(symbol);

        if (quote != null) {
            return ResponseEntity.ok(quote);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{symbol}/history")
    public ResponseEntity<?> getStockHistory(
            @PathVariable String symbol,
            @RequestParam(required = false, defaultValue = "1D") String range) {

        List<ChartPoint> history = marketDataService.getStockHistory(symbol, range);

        if (history != null && !history.isEmpty()) {
            return ResponseEntity.ok(history);
        } else {
            return ResponseEntity.badRequest().body("No historical data available.");
        }
    }
}
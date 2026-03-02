package com.thesis.stocktradingsimulator.controller;

import com.thesis.stocktradingsimulator.model.StockQuote;
import com.thesis.stocktradingsimulator.service.MarketDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
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
            return ResponseEntity.ok(quote); // Returns 200 OK + JSON data
        } else {
            return ResponseEntity.notFound().build(); // Returns 404 Error
        }
    }
}

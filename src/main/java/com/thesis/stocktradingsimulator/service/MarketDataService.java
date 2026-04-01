package com.thesis.stocktradingsimulator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesis.stocktradingsimulator.dto.ChartPoint;
import com.thesis.stocktradingsimulator.model.StockQuote;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class MarketDataService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Yahoo requires a User-Agent, otherwise it might block the request thinking it's a bot
    private HttpEntity<String> getYahooHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        return new HttpEntity<>(headers);
    }

    @Cacheable("livePrices")
    public StockQuote getLivePrice(String symbol) {
        // Yahoo's v8 chart API gives us the current market price in the "meta" object
        String url = "https://query2.finance.yahoo.com/v8/finance/chart/" + symbol.toUpperCase() + "?interval=1m&range=1d";

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, getYahooHeaders(), String.class);
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode resultNode = rootNode.path("chart").path("result").get(0);

            if (resultNode != null && !resultNode.isMissingNode()) {
                double currentPrice = resultNode.path("meta").path("regularMarketPrice").asDouble();
                return new StockQuote(symbol.toUpperCase(), currentPrice);
            }
        } catch (Exception e) {
            System.err.println("Error fetching live price for " + symbol + " from Yahoo: " + e.getMessage());
        }
        return null;
    }

    public List<ChartPoint> getStockHistory(String symbol, String range) {
        // Map your frontend ranges to Yahoo's required formats
        String yfRange = "1d";
        String yfInterval = "5m";

        if ("1W".equalsIgnoreCase(range)) {
            yfRange = "5d";
            yfInterval = "15m";
        } else if ("1M".equalsIgnoreCase(range)) {
            yfRange = "1mo";
            yfInterval = "1d";
        }

        String url = String.format("https://query2.finance.yahoo.com/v8/finance/chart/%s?range=%s&interval=%s",
                symbol.toUpperCase(), yfRange, yfInterval);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, getYahooHeaders(), String.class);
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode resultNode = rootNode.path("chart").path("result").get(0);

            if (resultNode != null && !resultNode.isMissingNode()) {
                JsonNode timestampArray = resultNode.path("timestamp");
                JsonNode closePriceArray = resultNode.path("indicators").path("quote").get(0).path("close");

                List<ChartPoint> history = new ArrayList<>();

                // Yahoo returns parallel arrays for timestamps and prices
                for (int i = 0; i < timestampArray.size(); i++) {
                    if (!closePriceArray.get(i).isNull()) {
                        // Yahoo timestamps are in seconds, Java uses milliseconds, so multiply by 1000
                        long timestampMs = timestampArray.get(i).asLong() * 1000;
                        double closePrice = closePriceArray.get(i).asDouble();

                        history.add(new ChartPoint(timestampMs, "", closePrice));
                    }
                }
                return history;
            }
        } catch (Exception e) {
            System.err.println("Error fetching historical data for " + symbol + " from Yahoo: " + e.getMessage());
        }

        return null;
    }
    @CacheEvict(value = "livePrices", allEntries = true)
    @Scheduled(fixedRate = 5000)//clear cache every 5s
    public void clearPriceCache() {
        // You can leave this empty! The annotations do all the work.
        System.out.println("Clearing the live price cache to fetch fresh Yahoo data...");
    }
}
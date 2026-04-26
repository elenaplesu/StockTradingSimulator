package com.thesis.stocktradingsimulator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesis.stocktradingsimulator.dto.ChartPoint;
import com.thesis.stocktradingsimulator.model.StockQuote;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class MarketDataService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public MarketDataService(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    private HttpRequest buildYahooRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .GET()
                .build();
    }

    @Cacheable("livePrices")
    @Retryable(
            value = { Exception.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2) // Wait 1s, then 2s, then fail
    )
    public StockQuote getLivePrice(String symbol) {
        String url = "https://query2.finance.yahoo.com/v8/finance/chart/" + symbol.toUpperCase() + "?interval=1m&range=1d";

        try {
            HttpRequest request = buildYahooRequest(url);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            JsonNode rootNode = objectMapper.readTree(response.body());
            JsonNode resultNode = rootNode.path("chart").path("result").get(0);

            if (resultNode != null && !resultNode.isMissingNode()) {
                String priceStr = resultNode.path("meta").path("regularMarketPrice").asText();
                BigDecimal currentPrice = new BigDecimal(priceStr);
                return new StockQuote(symbol.toUpperCase(), currentPrice);
            }
        } catch (Exception e) {
            System.err.println("Error fetching live price for " + symbol + " from Yahoo: " + e.getMessage());
            throw new RuntimeException("API call failed"); // Throwing triggers the retry!
        }
        throw new RuntimeException("Missing data from Yahoo");
    }

    // If all 3 retries fail, this method runs automatically
    @Recover
    public StockQuote recoverLivePrice(RuntimeException e, String symbol) {
        System.err.println("All retries failed for " + symbol + ". Returning null.");
        return null;
    }

    public List<ChartPoint> getStockHistory(String symbol, String range) {
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
            HttpRequest request = buildYahooRequest(url);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            JsonNode rootNode = objectMapper.readTree(response.body());
            JsonNode resultNode = rootNode.path("chart").path("result").get(0);

            if (resultNode != null && !resultNode.isMissingNode()) {
                JsonNode timestampArray = resultNode.path("timestamp");
                JsonNode closePriceArray = resultNode.path("indicators").path("quote").get(0).path("close");

                List<ChartPoint> history = new ArrayList<>();

                for (int i = 0; i < timestampArray.size(); i++) {
                    if (!closePriceArray.get(i).isNull()) {
                        long timestampMs = timestampArray.get(i).asLong() * 1000;

                        String closePriceStr = closePriceArray.get(i).asText();
                        BigDecimal closePrice = new BigDecimal(closePriceStr);

                        history.add(new ChartPoint(timestampMs, closePrice));
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
    @Scheduled(fixedRate = 5000)
    public void clearPriceCache() {
        System.out.println("Clearing the live price cache to fetch fresh Yahoo data...");
    }
}
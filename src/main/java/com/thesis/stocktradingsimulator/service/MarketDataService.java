package com.thesis.stocktradingsimulator.service;

import com.thesis.stocktradingsimulator.model.StockQuote;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class MarketDataService {
    @Value("${stock.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public StockQuote getLivePrice(String symbol) {
        String url = "https://finnhub.io/api/v1/quote?symbol=" + symbol.toUpperCase() + "&token=" + apiKey;
        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.get("c") != null) {
                // "c" is Finnhub's key for Current Price
                double currentPrice = Double.parseDouble(response.get("c").toString());

                // If price is 0, the symbol does not exist or there might have been a problem
                if (currentPrice == 0.0) return null;

                return new StockQuote(symbol.toUpperCase(), currentPrice);
            }
        }
        catch (Exception e) {
            System.out.println("Error fetching data for " + symbol + ": " + e.getMessage());
        }
        return null;
    }

}

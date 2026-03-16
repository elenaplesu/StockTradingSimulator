package com.thesis.stocktradingsimulator.service;

import com.thesis.stocktradingsimulator.dto.ChartPoint;
import com.thesis.stocktradingsimulator.model.StockQuote;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@Service
public class MarketDataService {
    @Value("${stock.api.key}")
    private String finnhubApiKey;

    @Value("${stock.api.key.2}")
    private String twelveDataApiKey ;
    private final RestTemplate restTemplate = new RestTemplate();

    public StockQuote getLivePrice(String symbol) {
        String url = "https://finnhub.io/api/v1/quote?symbol=" + symbol.toUpperCase() + "&token=" + finnhubApiKey;
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

    public List<ChartPoint> getStockHistory(String symbol, String range) {
        String interval = "5min";
        int outputsize = 288;

        if ("1W".equalsIgnoreCase(range)) {
            interval = "15min";
            outputsize = 150;
        }

        String url = String.format("https://api.twelvedata.com/time_series?symbol=%s&interval=%s&outputsize=%d&apikey=%s",
                symbol.toUpperCase(), interval, outputsize, twelveDataApiKey);

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && "ok".equals(response.get("status")) && response.containsKey("values")) {
                List<Map<String, String>> values = (List<Map<String, String>>) response.get("values");
                List<ChartPoint> history = new ArrayList<>();

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));

                for (int i = values.size() - 1; i >= 0; i--) {
                    Map<String, String> dataPoint = values.get(i);
                    String datetimeStr = dataPoint.get("datetime");
                    double closePrice = Double.parseDouble(dataPoint.get("close"));

                    long timestamp = sdf.parse(datetimeStr).getTime();
                    history.add(new ChartPoint(timestamp, "", closePrice));
                }

                return history;
            }
        } catch (Exception e) {
            System.err.println("Error fetching historical data for " + symbol + ": " + e.getMessage());
        }

        return null; // Return null if the fetch fails or data is invalid
    }

}

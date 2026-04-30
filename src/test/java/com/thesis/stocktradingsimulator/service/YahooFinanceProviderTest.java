package com.thesis.stocktradingsimulator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesis.stocktradingsimulator.dto.ChartPoint;
import com.thesis.stocktradingsimulator.model.StockQuote;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class YahooFinanceProviderTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> mockResponse;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private YahooFinanceProvider yahooFinanceProvider;

    @Test
    void getLivePrice_ShouldParseJsonCorrectly_WhenYahooReturnsValidData() throws Exception {
        String fakeYahooJson = """
            {
              "chart": {
                "result": [
                  {
                    "meta": {
                      "regularMarketPrice": 175.50
                    }
                  }
                ]
              }
            }
            """;

        when(mockResponse.body()).thenReturn(fakeYahooJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        StockQuote quote = yahooFinanceProvider.getLivePrice("AAPL");

        assertNotNull(quote, "StockQuote should not be null");
        assertEquals("AAPL", quote.getSymbol());
        assertEquals(new BigDecimal("175.5"), quote.getCurrentPrice());
    }

    @Test
    void getLivePrice_ShouldThrowException_WhenApiFailsOrTimesOut() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new java.net.http.HttpTimeoutException("Yahoo is down"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            yahooFinanceProvider.getLivePrice("AAPL");
        });

        assertEquals("API call failed", exception.getMessage());
    }

    @Test
    void recoverLivePrice_ShouldReturnNull_WhenAllRetriesFail() {

        RuntimeException fakeException = new RuntimeException("API call failed");
        StockQuote quote = yahooFinanceProvider.recoverLivePrice(fakeException, "AAPL");

        assertNull(quote, "Recovery method should safely return null");
    }

    @Test
    void getStockHistory_ShouldParseArraysCorrectly_WhenYahooReturnsValidData() throws Exception {

        String fakeHistoryJson = """
            {
              "chart": {
                "result": [
                  {
                    "timestamp": [1700000000, 1700000060],
                    "indicators": {
                      "quote": [
                        {
                          "close": [150.00, 151.25]
                        }
                      ]
                    }
                  }
                ]
              }
            }
            """;

        when(mockResponse.body()).thenReturn(fakeHistoryJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        List<ChartPoint> history = yahooFinanceProvider.getStockHistory("AAPL", "1D");

        assertNotNull(history);
        assertEquals(2, history.size(), "Should have parsed exactly two chart points");

        assertEquals(1700000000000L, history.get(0).timestamp());
        assertEquals(new BigDecimal("150.0"), history.get(0).price());

        assertEquals(1700000060000L, history.get(1).timestamp());
        assertEquals(new BigDecimal("151.25"), history.get(1).price());
    }

    @Test
    void getStockHistory_ShouldReturnNull_WhenJsonIsMalformed() throws Exception {

        when(mockResponse.body()).thenReturn("{ \"broken\": \"json\" }");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        List<ChartPoint> history = yahooFinanceProvider.getStockHistory("AAPL", "1D");

        assertNotNull(history);
        assertTrue(history.isEmpty(), "Should return empty list if JSON doesn't contain expected structure");
    }
}
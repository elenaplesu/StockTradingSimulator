package com.thesis.stocktradingsimulator.controller;

import com.thesis.stocktradingsimulator.dto.ChartPoint;
import com.thesis.stocktradingsimulator.model.StockQuote;
import com.thesis.stocktradingsimulator.service.MarketDataProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StockController.class)
@AutoConfigureMockMvc(addFilters = false)
class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MarketDataProvider marketDataProvider;

    @Test
    void getStockPrice_ShouldReturn200Ok_AndQuote_WhenSymbolIsValid() throws Exception {

        StockQuote mockQuote = new StockQuote("AAPL", new BigDecimal("175.50"));
        when(marketDataProvider.getLivePrice("AAPL")).thenReturn(mockQuote);

        mockMvc.perform(get("/api/stocks/AAPL")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.currentPrice").value(175.50));
    }

    @Test
    void getStockPrice_ShouldReturn404NotFound_WhenSymbolIsInvalid() throws Exception {

        when(marketDataProvider.getLivePrice("FAKE")).thenReturn(null);

        mockMvc.perform(get("/api/stocks/FAKE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Stock symbol not found."));
    }


    @Test
    void getStockHistory_ShouldReturn200Ok_AndHistoryList_WhenDataExists() throws Exception {

        long fakeTimestamp = 1700000000000L;
        ChartPoint fakePoint = new ChartPoint(fakeTimestamp, new BigDecimal("150.00"));
        when(marketDataProvider.getStockHistory("AAPL", "1W")).thenReturn(List.of(fakePoint));

        mockMvc.perform(get("/api/stocks/AAPL/history?range=1W")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].timestamp").value(fakeTimestamp))
                .andExpect(jsonPath("$[0].price").value(150.00));
    }

    @Test
    void getStockHistory_ShouldUseDefaultRange1D_WhenNoRangeIsProvided() throws Exception {
        ChartPoint fakePoint = new ChartPoint(123456L, new BigDecimal("100.00"));
        when(marketDataProvider.getStockHistory("TSLA", "1D")).thenReturn(List.of(fakePoint));

        mockMvc.perform(get("/api/stocks/TSLA/history")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void getStockHistory_ShouldReturn400BadRequest_WhenDataIsEmpty() throws Exception {

        when(marketDataProvider.getStockHistory("AAPL", "1M")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/stocks/AAPL/history?range=1M")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No historical data available."));
    }
}
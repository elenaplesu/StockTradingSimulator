package com.thesis.stocktradingsimulator.controller;

import com.thesis.stocktradingsimulator.exception.InsufficientFundsException;
import com.thesis.stocktradingsimulator.exception.InsufficientSharesException;
import com.thesis.stocktradingsimulator.model.Transaction;
import com.thesis.stocktradingsimulator.service.TransactionManagerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TradeController.class)
@AutoConfigureMockMvc(addFilters = false)
class TradeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionManagerService transactionManagerService;

    @Test
    void buyStock_ShouldReturn200Ok_AndFormattedSuccessMessage() throws Exception {

        Transaction mockTx = mock(Transaction.class);
        when(mockTx.getSymbol()).thenReturn("AAPL");
        when(mockTx.getQuantity()).thenReturn(10);
        when(mockTx.getExecutionPrice()).thenReturn(new BigDecimal("150.00"));

        when(transactionManagerService.executeBuy(1L, "AAPL", 10)).thenReturn(mockTx);

        String requestJson = """
                {
                    "userId": 1,
                    "symbol": "AAPL",
                    "quantity": 10
                }
                """;

        mockMvc.perform(post("/api/trade/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Success: Bought 10 shares of AAPL for $1500.00"));
    }

    @Test
    void sellStock_ShouldReturn200Ok_AndFormattedSuccessMessage() throws Exception {

        Transaction mockTx = mock(Transaction.class);
        when(mockTx.getSymbol()).thenReturn("TSLA");
        when(mockTx.getQuantity()).thenReturn(5);
        when(mockTx.getExecutionPrice()).thenReturn(new BigDecimal("200.00"));

        when(transactionManagerService.executeSell(1L, "TSLA", 5)).thenReturn(mockTx);

        String requestJson = """
                {
                    "userId": 1,
                    "symbol": "TSLA",
                    "quantity": 5
                }
                """;

        mockMvc.perform(post("/api/trade/sell")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Success: Sold 5 shares of TSLA for $1000.00"));
    }
}
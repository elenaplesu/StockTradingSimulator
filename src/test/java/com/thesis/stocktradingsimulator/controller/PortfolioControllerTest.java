package com.thesis.stocktradingsimulator.controller;

import com.thesis.stocktradingsimulator.dto.PortfolioAnalyticsDTO;
import com.thesis.stocktradingsimulator.model.Holding;
import com.thesis.stocktradingsimulator.model.Portfolio;
import com.thesis.stocktradingsimulator.model.Transaction;
import com.thesis.stocktradingsimulator.model.User;
import com.thesis.stocktradingsimulator.repository.HoldingRepository;
import com.thesis.stocktradingsimulator.service.PortfolioAnalyticsService;
import com.thesis.stocktradingsimulator.service.TransactionManagerService;
import com.thesis.stocktradingsimulator.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PortfolioController.class)
@AutoConfigureMockMvc(addFilters = false)
class PortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private UserService userService;
    @MockitoBean private HoldingRepository holdingRepository;
    @MockitoBean private PortfolioAnalyticsService analyticsService;
    @MockitoBean private TransactionManagerService transactionManagerService;

    private User mockUser;
    private Portfolio mockPortfolio;

    @BeforeEach
    void setUp() {
        mockUser = new User("testStudent", "password123", new BigDecimal("10000.00"));
        mockUser.setId(1L);

        mockPortfolio = new Portfolio(mockUser);
        mockPortfolio.setId(100L);
    }

    @Test
    void getPortfolioData_ShouldReturn200Ok_AndPortfolioMap_WhenUserExists() throws Exception {
        Holding appleHolding = new Holding(mockPortfolio, "AAPL", 5, new BigDecimal("150.00"));
        List<Holding> mockHoldings = List.of(appleHolding);

        when(userService.getUserById(1L)).thenReturn(mockUser);
        when(userService.getPortfolioByUserId(1L)).thenReturn(mockPortfolio);
        when(holdingRepository.findByPortfolioId(100L)).thenReturn(mockHoldings);

        mockMvc.perform(get("/api/portfolio/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashBalance").value(10000.00))
                .andExpect(jsonPath("$.holdings[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$.holdings[0].quantity").value(5));
    }

    @Test
    void getPortfolioData_ShouldReturn404NotFound_WhenUserIsNull() throws Exception {
        when(userService.getUserById(99L)).thenReturn(null);

        mockMvc.perform(get("/api/portfolio/99")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPortfolioAnalytics_ShouldReturn200Ok_AndAnalyticsDTO() throws Exception {
        PortfolioAnalyticsDTO mockAnalytics = new PortfolioAnalyticsDTO(
                new BigDecimal("10000.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("15000.00"),
                new BigDecimal("10.5"),
                new BigDecimal("2500.00"),
                new BigDecimal("0.5"),
                new ArrayList<>()
        );

        when(analyticsService.generateAnalytics(1L)).thenReturn(mockAnalytics);

        mockMvc.perform(get("/api/portfolio/1/analytics")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netWorth").value(15000.00))
                .andExpect(jsonPath("$.totalEquity").value(5000.00))
                .andExpect(jsonPath("$.aggregateROI").value(10.5));
    }

    @Test
    void getTransactionHistory_ShouldReturn200Ok_AndTransactionList() throws Exception {
        Transaction mockTx = new Transaction(mockPortfolio, Transaction.TransactionType.BUY, "TSLA", 10, new BigDecimal("200.00"));
        when(transactionManagerService.getTransactionHistory(1L)).thenReturn(List.of(mockTx));

        mockMvc.perform(get("/api/portfolio/1/transactions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("TSLA"))
                .andExpect(jsonPath("$[0].quantity").value(10))
                .andExpect(jsonPath("$[0].type").value("BUY"));
    }
}
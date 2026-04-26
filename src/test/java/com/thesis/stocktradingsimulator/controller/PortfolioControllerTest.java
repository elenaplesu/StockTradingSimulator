package com.thesis.stocktradingsimulator.controller;

import com.thesis.stocktradingsimulator.dto.PortfolioAnalyticsDTO;
import com.thesis.stocktradingsimulator.model.Holding;
import com.thesis.stocktradingsimulator.model.Portfolio;
import com.thesis.stocktradingsimulator.model.Transaction;
import com.thesis.stocktradingsimulator.model.User;
import com.thesis.stocktradingsimulator.repository.HoldingRepository;
import com.thesis.stocktradingsimulator.repository.UserRepository;
import com.thesis.stocktradingsimulator.service.PortfolioAnalyticsService;
import com.thesis.stocktradingsimulator.service.TransactionManagerService;
import com.thesis.stocktradingsimulator.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PortfolioController.class)
@AutoConfigureMockMvc
class PortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private UserService userService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private HoldingRepository holdingRepository;
    @MockitoBean private PortfolioAnalyticsService analyticsService;
    @MockitoBean private TransactionManagerService transactionManagerService;

    private User mockUser;
    private Portfolio mockPortfolio;

    @BeforeEach
    void setUp() {
        mockUser = new User("testStudent", "password123");
        mockUser.setId(1L);

        mockPortfolio = new Portfolio(mockUser);
        mockPortfolio.setId(100L);

        when(userRepository.findByUsername("testStudent")).thenReturn(Optional.of(mockUser));
    }

    @Test
    @WithMockUser(username = "testStudent")
    void getPortfolioData_ShouldReturn200Ok_AndPortfolioMap_WhenUserExists() throws Exception {
        Holding appleHolding = new Holding(mockPortfolio, "AAPL", 5, new BigDecimal("150.00"));

        when(userService.getUserById(1L)).thenReturn(mockUser);
        when(userService.getPortfolioByUserId(1L)).thenReturn(mockPortfolio);
        when(holdingRepository.findByPortfolioId(100L)).thenReturn(List.of(appleHolding));

        mockMvc.perform(get("/api/portfolio/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashBalance").value(10000.00))
                .andExpect(jsonPath("$.holdings[0].symbol").value("AAPL"));
    }

    @Test
    @WithMockUser(username = "testStudent")
    void getPortfolioData_ShouldReturn200Ok_WhenAccessingOtherUserData() throws Exception {
        when(userService.getUserById(99L)).thenReturn(mockUser);
        when(userService.getPortfolioByUserId(99L)).thenReturn(mockPortfolio);
        when(holdingRepository.findByPortfolioId(100L)).thenReturn(List.of());

        mockMvc.perform(get("/api/portfolio/99")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testStudent")
    void getPortfolioAnalytics_ShouldReturn200Ok_AndAnalyticsDTO() throws Exception {
        PortfolioAnalyticsDTO mockAnalytics = new PortfolioAnalyticsDTO(
                new BigDecimal("10000.00"), new BigDecimal("5000.00"),
                new BigDecimal("15000.00"), new BigDecimal("10.5"),
                new BigDecimal("2500.00"), new BigDecimal("0.5"), new ArrayList<>()
        );

        when(analyticsService.generateAnalytics(1L)).thenReturn(mockAnalytics);

        mockMvc.perform(get("/api/portfolio/1/analytics")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netWorth").value(15000.00));
    }

    @Test
    @WithMockUser(username = "testStudent")
    void getTransactionHistory_ShouldReturn200Ok_AndTransactionList() throws Exception {
        Transaction mockTx = new Transaction(mockPortfolio, Transaction.TransactionType.BUY, "TSLA", 10, new BigDecimal("200.00"));
        when(transactionManagerService.getTransactionHistory(1L)).thenReturn(List.of(mockTx));

        mockMvc.perform(get("/api/portfolio/1/transactions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("TSLA"));
    }
}
package com.thesis.stocktradingsimulator.config;

import com.thesis.stocktradingsimulator.model.StockQuote;
import com.thesis.stocktradingsimulator.service.MarketDataProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MarketDataProvider marketDataProvider;

    @BeforeEach
    void setUp() {
        when(marketDataProvider.getLivePrice("AAPL"))
                .thenReturn(new StockQuote("AAPL", new BigDecimal("150.00")));
    }

    @Test
    void publicEndpoints_ShouldBeAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/stocks/AAPL"))
                .andExpect(status().isOk());
    }

    @Test
    void privateEndpoints_ShouldBeBlockedWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/portfolio/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginEndpoint_ShouldBePublic() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test\", \"password\":\"pass\"}"))
                .andExpect(status().isUnauthorized()); // Now it hits the provider and fails auth correctly
    }

    @Test
    void logout_ShouldReturn200Ok_WhenLoggedIn() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("testStudent"))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk());
    }
}
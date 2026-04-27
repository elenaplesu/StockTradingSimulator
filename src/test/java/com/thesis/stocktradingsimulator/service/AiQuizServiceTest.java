package com.thesis.stocktradingsimulator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesis.stocktradingsimulator.model.Portfolio;
import com.thesis.stocktradingsimulator.model.User;
import com.thesis.stocktradingsimulator.repository.HoldingRepository;
import com.thesis.stocktradingsimulator.repository.PortfolioRepository;
import com.thesis.stocktradingsimulator.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiQuizServiceTest {

    @Mock private PortfolioRepository portfolioRepository;
    @Mock private HoldingRepository holdingRepository;
    @Mock private HttpClient httpClient;
    @Mock private HttpResponse<String> mockResponse;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AiQuizService aiQuizService;

    private User mockUser;
    private Portfolio mockPortfolio;

    @BeforeEach
    void setUp() {
        mockUser = TestDataFactory.createStandardMockUser();
        mockPortfolio = TestDataFactory.createStandardMockPortfolio(mockUser);
        ReflectionTestUtils.setField(aiQuizService, "apiKey", "test-api-key");
    }

    @Test
    void generatePersonalizedQuiz_ShouldReturnParsedQuestions_WhenApiSucceeds() throws Exception {

        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(mockPortfolio));
        when(holdingRepository.findByPortfolioId(1L)).thenReturn(List.of());

        String fakeGroqResponse = """
            {
              "choices": [
                {
                  "message": {
                    "content": "{\\"questions\\": [{\\"q\\": \\"Fake AI Question?\\", \\"options\\": [\\"A\\", \\"B\\", \\"C\\", \\"D\\"], \\"answer\\": \\"A\\"}]}"
                  }
                }
              ]
            }
            """;

        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(fakeGroqResponse);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        String result = aiQuizService.generatePersonalizedQuiz(1L);

        assertNotNull(result);
        assertTrue(result.contains("Fake AI Question?"), "The result should contain the mocked AI text");

        verify(httpClient, times(1)).send(any(), any());
    }

    @Test
    void generatePersonalizedQuiz_ShouldReturnFallback_WhenApiTimesOut() throws Exception {

        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(mockPortfolio));
        when(holdingRepository.findByPortfolioId(1L)).thenReturn(List.of());

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new java.net.http.HttpTimeoutException("Connection timed out"));

        String result = aiQuizService.generatePersonalizedQuiz(1L);

        assertNotNull(result);
        assertTrue(result.startsWith("[") && result.endsWith("]"), "Fallback should return a JSON array");

        verify(httpClient, times(1)).send(any(), any());
    }

    @Test
    void getFallbackQuiz_ShouldAlwaysReturnExactlyFiveQuestions() {

        String result = aiQuizService.getFallbackQuiz();

        assertNotNull(result);
        assertTrue(result.startsWith("[") && result.endsWith("]"));

        try {
            List<?> parsedList = objectMapper.readValue(result, List.class);
            assertEquals(5, parsedList.size(), "The fallback pool should always select exactly 5 questions");
        } catch (Exception e) {
            fail("Fallback quiz did not generate valid JSON");
        }
    }
}
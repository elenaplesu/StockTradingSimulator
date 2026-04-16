package com.thesis.stocktradingsimulator.controller;

import com.thesis.stocktradingsimulator.service.AiQuizService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LearnController.class)
@AutoConfigureMockMvc(addFilters = false)
class LearnControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiQuizService aiQuizService;

    @Test
    void getPersonalizedQuiz_ShouldReturn200Ok_AndQuizJson() throws Exception {
        String mockAiResponse = "[{\"q\": \"What is a stock?\", \"options\": [\"A\", \"B\"], \"answer\": \"A\"}]";
        when(aiQuizService.generatePersonalizedQuiz(1L)).thenReturn(mockAiResponse);

        mockMvc.perform(get("/api/learn/ai-quiz/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(mockAiResponse));

        verify(aiQuizService).generatePersonalizedQuiz(1L);
    }

    @Test
    void getGeneralQuiz_ShouldReturn200Ok_AndFallbackQuizJson() throws Exception {

        String mockFallbackResponse = "[{\"q\": \"What is an ETF?\", \"options\": [\"1\", \"2\"], \"answer\": \"1\"}]";
        when(aiQuizService.getFallbackQuiz()).thenReturn(mockFallbackResponse);

        mockMvc.perform(get("/api/learn/general-quiz")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(mockFallbackResponse));

        verify(aiQuizService).getFallbackQuiz();
    }
}
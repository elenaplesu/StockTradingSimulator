package com.thesis.stocktradingsimulator.controller;

import com.thesis.stocktradingsimulator.service.AiQuizService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/learn")
public class LearnController {

    private final AiQuizService aiQuizService;

    public LearnController(AiQuizService aiQuizService) {
        this.aiQuizService = aiQuizService;
    }

    @GetMapping("/ai-quiz/{userId}")
    public ResponseEntity<String> getPersonalizedQuiz(@PathVariable Long userId) {
        String quizJson = aiQuizService.generatePersonalizedQuiz(userId);
        return ResponseEntity.ok(quizJson);
    }

    @GetMapping("/general-quiz")
    public ResponseEntity<String> getGeneralQuiz() {
        return ResponseEntity.ok(aiQuizService.getFallbackQuiz());
    }
}
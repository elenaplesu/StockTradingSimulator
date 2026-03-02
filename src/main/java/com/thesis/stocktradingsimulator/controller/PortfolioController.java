package com.thesis.stocktradingsimulator.controller;

import com.thesis.stocktradingsimulator.model.Portfolio;
import com.thesis.stocktradingsimulator.model.User;
import com.thesis.stocktradingsimulator.repository.HoldingRepository;
import com.thesis.stocktradingsimulator.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
@RestController
@RequestMapping("/api/portfolio")
@CrossOrigin(origins = "http://localhost:5173") // Allows React to talk to this endpoint
public class PortfolioController {
    private final UserService userService;
    private final HoldingRepository holdingRepository;

    public PortfolioController(UserService userService, HoldingRepository holdingRepository) {
        this.userService = userService;
        this.holdingRepository = holdingRepository;
    }
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getPortfolioData(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        Portfolio portfolio = userService.getPortfolioByUserId(userId);

        if (user == null || portfolio == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("cashBalance", user.getCashBalance());
        response.put("holdings", holdingRepository.findByPortfolioId(portfolio.getId()));

        return ResponseEntity.ok(response);
    }
}

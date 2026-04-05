package com.thesis.stocktradingsimulator.controller;

import com.thesis.stocktradingsimulator.model.Portfolio; // NEW IMPORT
import com.thesis.stocktradingsimulator.model.User;
import com.thesis.stocktradingsimulator.repository.PortfolioRepository; // NEW IMPORT
import com.thesis.stocktradingsimulator.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;

    public UserController(UserRepository userRepository, PortfolioRepository portfolioRepository) {
        this.userRepository = userRepository;
        this.portfolioRepository = portfolioRepository;
    }

    public static class LoginRequest {
        public String username;
        public String password;
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest request) {
        Optional<User> userOptional = userRepository.findByUsername(request.username);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (user.getPassword().equals(request.password)) {
                return ResponseEntity.ok(user.getId());
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody LoginRequest request) {
        if (userRepository.findByUsername(request.username).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists.");
        }

        // 1. Create the new user
        User newUser = new User();
        newUser.setUsername(request.username);
        newUser.setPassword(request.password);
        newUser.setCashBalance(10000.0);
        User savedUser = userRepository.save(newUser);

        // 2. NEW: Create an empty portfolio and attach it to the new user!
        Portfolio newPortfolio = new Portfolio();
        newPortfolio.setUser(savedUser); // Links the portfolio to this specific user
        portfolioRepository.save(newPortfolio);

        // 3. Send back the User ID
        return ResponseEntity.ok(savedUser.getId());
    }
}
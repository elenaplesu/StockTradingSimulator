package com.thesis.stocktradingsimulator.service;

import com.thesis.stocktradingsimulator.exception.ResourceNotFoundException;
import com.thesis.stocktradingsimulator.exception.UserAlreadyExistsException;
import com.thesis.stocktradingsimulator.model.Portfolio;
import com.thesis.stocktradingsimulator.model.User;
import com.thesis.stocktradingsimulator.repository.PortfolioRepository;
import com.thesis.stocktradingsimulator.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PortfolioRepository portfolioRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.portfolioRepository = portfolioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerNewUser(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException("Username '" + username + "' is already taken.");
        }

        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }

        User newUser = new User(
                username,
                passwordEncoder.encode(password)
        );

        User savedUser = userRepository.save(newUser);

        Portfolio newPortfolio = new Portfolio(savedUser);
        portfolioRepository.save(newPortfolio);

        return savedUser;
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
    }

    public Portfolio getPortfolioByUserId(Long userId) {
        return portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found for User ID: " + userId));
    }
}
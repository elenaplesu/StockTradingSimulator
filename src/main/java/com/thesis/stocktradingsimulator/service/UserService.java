package com.thesis.stocktradingsimulator.service;

import com.thesis.stocktradingsimulator.model.Portfolio;
import com.thesis.stocktradingsimulator.model.User;
import com.thesis.stocktradingsimulator.repository.PortfolioRepository;
import com.thesis.stocktradingsimulator.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;

    public UserService(UserRepository userRepository, PortfolioRepository portfolioRepository) {
        this.userRepository = userRepository;
        this.portfolioRepository = portfolioRepository;
    }

    public String registerNewUser(String username, String password) {
        // 1. Check if username is already taken
        if (userRepository.existsByUsername(username)) {
            return "Error: Username already exists!";
        }
        // 2. Create the User with a $10,000 starting balance
        User newUser = new User(username, password, 10000.0);
        userRepository.save(newUser);
        // 3. Create an empty Portfolio attached to this user
        Portfolio newPortfolio = new Portfolio(newUser, 10000.0);
        portfolioRepository.save(newPortfolio);

        return "Success";
    }

    public User authenticateUser(String username, String password) {
        java.util.Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
           //note to add password protection
            if (user.getPassword().equals(password)) {
                return user;
            }
        }
        return null; // Failed login
    }
    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public Portfolio getPortfolioByUserId(Long userId) {
        return portfolioRepository.findByUserId(userId).orElse(null);
    }

}

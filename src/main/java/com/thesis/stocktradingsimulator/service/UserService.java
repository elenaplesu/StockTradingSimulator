package com.thesis.stocktradingsimulator.service;

import com.thesis.stocktradingsimulator.model.Portfolio;
import com.thesis.stocktradingsimulator.model.User;
import com.thesis.stocktradingsimulator.repository.PortfolioRepository;
import com.thesis.stocktradingsimulator.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PortfolioRepository portfolioRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.portfolioRepository = portfolioRepository;
        this.passwordEncoder=passwordEncoder;
    }

    public String registerNewUser(String username, String password) {
        // 1. Check if username is already taken
        if (userRepository.existsByUsername(username)) {
            return "Error: Username already exists!";
        }
        // 2. Create the User with a $10,000 starting balance
        String hashedPwd = passwordEncoder.encode(password);
        User newUser = new User(username, hashedPwd, 10000.0);
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
            if (passwordEncoder.matches(password, user.getPassword())){
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

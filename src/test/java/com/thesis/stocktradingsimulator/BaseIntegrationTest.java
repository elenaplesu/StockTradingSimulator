package com.thesis.stocktradingsimulator;

import com.thesis.stocktradingsimulator.model.Portfolio;
import com.thesis.stocktradingsimulator.model.User;
import com.thesis.stocktradingsimulator.repository.PortfolioRepository;
import com.thesis.stocktradingsimulator.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PortfolioRepository portfolioRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected User defaultTestUser;
    protected Portfolio defaultPortfolio;

    @BeforeEach
    void setupBaseData() {
        User user = new User();
        user.setUsername("student1");
        user.setPassword(passwordEncoder.encode("securePassword123"));
        defaultTestUser = userRepository.save(user);

        Portfolio portfolio = new Portfolio();
        portfolio.setUser(defaultTestUser);
        defaultPortfolio = portfolioRepository.save(portfolio);
    }
}
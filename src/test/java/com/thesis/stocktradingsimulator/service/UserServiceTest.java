package com.thesis.stocktradingsimulator.service;

import com.thesis.stocktradingsimulator.exception.ResourceNotFoundException;
import com.thesis.stocktradingsimulator.exception.UserAlreadyExistsException;
import com.thesis.stocktradingsimulator.model.Portfolio;
import com.thesis.stocktradingsimulator.model.User;
import com.thesis.stocktradingsimulator.repository.PortfolioRepository;
import com.thesis.stocktradingsimulator.repository.UserRepository;
import com.thesis.stocktradingsimulator.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PortfolioRepository portfolioRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User mockUser;
    private Portfolio mockPortfolio;

    @BeforeEach
    void setUp() {
        mockUser = TestDataFactory.createStandardMockUser();
        mockUser.setPassword("hashedPassword123");
        mockPortfolio = TestDataFactory.createStandardMockPortfolio(mockUser);
    }

    @Test
    void registerNewUser_ShouldCreateUserAndPortfolio_WhenUsernameIsUnique() {
        when(userRepository.existsByUsername("newStudent")).thenReturn(false);
        when(passwordEncoder.encode("rawPassword")).thenReturn("hashedPassword123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.registerNewUser("newStudent", "rawPassword");

        assertNotNull(result);
        assertEquals("newStudent", result.getUsername());
        assertEquals("hashedPassword123", result.getPassword());

        verify(userRepository, times(1)).save(any(User.class));
        verify(portfolioRepository, times(1)).save(any(Portfolio.class));
    }

    @Test
    void registerNewUser_ShouldThrowException_WhenUsernameAlreadyExists() {
        when(userRepository.existsByUsername("testStudent")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> {
            userService.registerNewUser("testStudent", "password123");
        });

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
        verify(portfolioRepository, never()).save(any());
    }


    @Test
    void getUserById_ShouldThrowException_WhenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            userService.getUserById(99L);
        });
    }

    @Test
    void getPortfolioByUserId_ShouldThrowException_WhenNotFound() {
        when(portfolioRepository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            userService.getPortfolioByUserId(99L);
        });
    }
}
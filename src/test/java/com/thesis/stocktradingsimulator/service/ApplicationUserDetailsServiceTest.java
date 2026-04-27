package com.thesis.stocktradingsimulator.service;

import com.thesis.stocktradingsimulator.model.User;
import com.thesis.stocktradingsimulator.repository.UserRepository;
import com.thesis.stocktradingsimulator.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ApplicationUserDetailsService userDetailsService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = TestDataFactory.createStandardMockUser();
        mockUser.setPassword("encodedPassword123");
    }

    @Test
    void loadUserByUsername_ShouldReturnUserDetails_WhenUserExists() {

        when(userRepository.findByUsername("testStudent")).thenReturn(Optional.of(mockUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("testStudent");

        assertNotNull(userDetails, "UserDetails should not be null");
        assertEquals("testStudent", userDetails.getUsername(), "Username mapping failed");
        assertEquals("encodedPassword123", userDetails.getPassword(), "Password mapping failed");
        assertEquals(1, userDetails.getAuthorities().size());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));assertEquals("testStudent", userDetails.getUsername(), "Username mapping failed");
        assertEquals("encodedPassword123", userDetails.getPassword(), "Password mapping failed");

        verify(userRepository, times(1)).findByUsername("testStudent");
    }

    @Test
    void loadUserByUsername_ShouldThrowException_WhenUserDoesNotExist() {

        when(userRepository.findByUsername("hacker")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername("hacker");
        });

        verify(userRepository, times(1)).findByUsername("hacker");
    }
}
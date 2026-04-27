package com.thesis.stocktradingsimulator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesis.stocktradingsimulator.exception.UserAlreadyExistsException;
import com.thesis.stocktradingsimulator.model.User;
import com.thesis.stocktradingsimulator.repository.PortfolioRepository;
import com.thesis.stocktradingsimulator.repository.UserRepository;
import com.thesis.stocktradingsimulator.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean private AuthenticationManager authenticationManager;
    @MockitoBean private SecurityContextRepository securityContextRepository;
    @MockitoBean private UserService userService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private PortfolioRepository portfolioRepository;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User("testStudent", "hashedPassword123");
        mockUser.setId(99L);
    }

    @Test
    void registerUser_ShouldReturn200Ok_WithUserId_WhenRegistrationIsSuccessful() throws Exception {

        when(userRepository.findByUsername("testStudent")).thenReturn(Optional.of(mockUser));
        when(userService.registerNewUser("testStudent", "password123")).thenReturn(mockUser);

        String requestJson = """
                {
                    "username": "testStudent",
                    "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().string("99"));
    }

    @Test
    void registerUser_ShouldReturn409Conflict_WhenUsernameIsTaken() throws Exception {
        when(userService.registerNewUser("takenUser", "password123"))
                .thenThrow(new UserAlreadyExistsException("Username 'takenUser' is already taken."));

        String requestJson = """
            {
                "username": "takenUser",
                "password": "password123"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username 'takenUser' is already taken."));
    }

    @Test
    void loginUser_ShouldReturn200Ok_WithUserId_WhenCredentialsAreValid() throws Exception {

        Authentication mockAuth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(userRepository.findByUsername("testStudent")).thenReturn(Optional.of(mockUser));

        String requestJson = """
                {
                    "username": "testStudent",
                    "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().string("99"));
    }

    @Test
    void loginUser_ShouldReturn401Unauthorized_WhenCredentialsAreInvalid() throws Exception {

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        String requestJson = """
                {
                    "username": "testStudent",
                    "password": "wrongPassword"
                }
                """;


        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

}
package com.thesis.stocktradingsimulator.repository;

import com.thesis.stocktradingsimulator.BaseIntegrationTest;
import com.thesis.stocktradingsimulator.model.User;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UserRepositoryTest extends BaseIntegrationTest {

    @Test
    void findByUsername_ShouldReturnUser_WhenUserExists() {
        Optional<User> foundUser = userRepository.findByUsername("student1");

        assertTrue(foundUser.isPresent(), "User should be found by username");
        assertEquals("student1", foundUser.get().getUsername());
    }

    @Test
    void findByUsername_ShouldReturnEmpty_WhenUserDoesNotExist() {

        Optional<User> foundUser = userRepository.findByUsername("non_existent_user");

        assertTrue(foundUser.isEmpty(), "Should return empty for a username that doesn't exist");
    }

    @Test
    void existsByUsername_ShouldReturnTrue_WhenUsernameIsTaken() {

        boolean exists = userRepository.existsByUsername("student1");

        assertTrue(exists, "Should return true for an existing username");
    }

    @Test
    void existsByUsername_ShouldReturnFalse_WhenUsernameIsAvailable() {

        boolean exists = userRepository.existsByUsername("fresh_new_user");

        assertFalse(exists, "Should return false for a new, available username");
    }
}
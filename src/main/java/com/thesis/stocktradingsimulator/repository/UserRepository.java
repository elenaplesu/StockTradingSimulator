package com.thesis.stocktradingsimulator.repository;

import com.thesis.stocktradingsimulator.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Check if a username is taken before registering
    boolean existsByUsername(String username);

    Optional<User> findByUsername(String username);
}

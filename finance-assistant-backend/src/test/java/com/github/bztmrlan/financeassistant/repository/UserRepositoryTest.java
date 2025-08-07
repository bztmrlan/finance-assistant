package com.github.bztmrlan.financeassistant.repository;

import com.github.bztmrlan.financeassistant.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setup() {
        user = new User();
        user.setName("Alice Johnson");
        user.setEmail("alice@example.com");
        user.setPassword("encrypted-password");
        user.setCreatedAt(Instant.now());
    }

    @Test
    @DisplayName("Should save and retrieve user by email")
    void testFindByEmail() {
        userRepository.save(user);
        Optional<User> retrieved = userRepository.findByEmail("alice@example.com");

        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getName()).isEqualTo("Alice Johnson");
        assertThat(retrieved.get().getCreatedAt()).isNotNull();
    }
}

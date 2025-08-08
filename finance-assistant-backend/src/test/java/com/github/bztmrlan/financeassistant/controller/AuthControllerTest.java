package com.github.bztmrlan.financeassistant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.bztmrlan.financeassistant.model.User;
import com.github.bztmrlan.financeassistant.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
public class AuthControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
        userRepository.deleteAll();
    }

    @Test
    void testLoginWithValidCredentials() throws Exception {

        User user = User.builder()
                .name("Test User")
                .email("test@example.com")
                .password(passwordEncoder.encode("password123"))
                .createdAt(Instant.now())
                .build();
        userRepository.save(user);


        AuthController.AuthRequest authRequest = new AuthController.AuthRequest();
        authRequest.setEmail("test@example.com");
        authRequest.setPassword("password123");


        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    void testLoginWithInvalidCredentials() throws Exception {

        User user = User.builder()
                .name("Test User")
                .email("test@example.com")
                .password(passwordEncoder.encode("password123"))
                .createdAt(Instant.now())
                .build();
        userRepository.save(user);


        AuthController.AuthRequest authRequest = new AuthController.AuthRequest();
        authRequest.setEmail("test@example.com");
        authRequest.setPassword("wrongpassword");


        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid credentials"));
    }

    @Test
    void testLoginWithNonExistentUser() throws Exception {
        // Create login request for non-existent user
        AuthController.AuthRequest authRequest = new AuthController.AuthRequest();
        authRequest.setEmail("nonexistent@example.com");
        authRequest.setPassword("password123");

        // Perform login request
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid credentials"));
    }

    @Test
    void testLoginWithEmptyCredentials() throws Exception {
        // Create login request with empty credentials
        AuthController.AuthRequest authRequest = new AuthController.AuthRequest();
        authRequest.setEmail("");
        authRequest.setPassword("");

        // Perform login request
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isUnauthorized());
    }
} 
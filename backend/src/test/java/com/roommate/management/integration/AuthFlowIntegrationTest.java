package com.roommate.management.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_thenLogin_succeeds() throws Exception {
        Map<String, String> registerBody = Map.of(
                "fullName", "Test User",
                "email", "testuser@example.com",
                "password", "password123",
                "mobileNumber", "9999999999"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.email").value("testuser@example.com"));

        Map<String, String> loginBody = Map.of("email", "testuser@example.com", "password", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void login_withWrongPassword_returnsUnauthorized() throws Exception {
        Map<String, String> registerBody = Map.of(
                "fullName", "Another User",
                "email", "another@example.com",
                "password", "correctPassword"
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isOk());

        Map<String, String> loginBody = Map.of("email", "another@example.com", "password", "wrongPassword");
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void duplicateRegistration_returnsConflict() throws Exception {
        Map<String, String> body = Map.of("fullName", "Dup User", "email", "dup@example.com", "password", "password123");
        mockMvc.perform(post("/api/auth/register").contentType("application/json").content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/register").contentType("application/json").content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict());
    }
}

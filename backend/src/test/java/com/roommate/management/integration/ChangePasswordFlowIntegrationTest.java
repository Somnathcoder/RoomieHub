package com.roommate.management.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.roommate.management.entity.User;
import com.roommate.management.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Reproduces the exact production-reported flow: admin creates a member (temp password
 * auto-generated), the member logs in, is forced to change the temp password, and must then
 * be able to log in again with the NEW password - the old temp password must stop working,
 * and the session issued at login must not be treated as expired by the change itself.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ChangePasswordFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void adminCreatedMember_forcedPasswordChange_thenLoginWithNewPassword_succeeds() throws Exception {
        // Admin registers and creates a room
        Map<String, String> adminBody = Map.of(
                "fullName", "Admin User", "email", "cpf-admin@example.com",
                "password", "adminPass1", "mobileNumber", "9111111111"
        );
        MvcResult adminResult = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json").content(objectMapper.writeValueAsString(adminBody)))
                .andExpect(status().isOk()).andReturn();
        String adminToken = JsonPath.read(adminResult.getResponse().getContentAsString(), "$.data.token");

        mockMvc.perform(post("/api/rooms").header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("roomName", "Test Room", "address", "1 Test St"))))
                .andExpect(status().isOk());

        // Admin adds a member WITHOUT a password -> backend must auto-generate a temporary one
        Map<String, Object> memberBody = Map.of(
                "fullName", "New Member", "email", "cpf-member@example.com", "role", "MEMBER", "mobileNumber", "9123456789"
        );
        MvcResult addResult = mockMvc.perform(post("/api/members").header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json").content(objectMapper.writeValueAsString(memberBody)))
                .andExpect(status().isOk())
                .andReturn();
        String tempPassword = JsonPath.read(addResult.getResponse().getContentAsString(), "$.data.temporaryPassword");
        assertNotNull(tempPassword, "addMember must return a generated temporary password exactly once");

        // Member logs in with the temporary password
        MvcResult firstLoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("email", "cpf-member@example.com", "password", tempPassword))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mustChangePassword").value(true))
                .andReturn();
        String firstLoginToken = JsonPath.read(firstLoginResult.getResponse().getContentAsString(), "$.data.token");

        // Member changes the password using the session/token from that login
        String newPassword = "NewPassword1";
        mockMvc.perform(post("/api/auth/change-password").header("Authorization", "Bearer " + firstLoginToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("currentPassword", tempPassword, "newPassword", newPassword))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // The change-password call must NOT have invalidated the token that was already valid -
        // the same token must still authenticate a normal request right after the change.
        mockMvc.perform(get("/api/members").header("Authorization", "Bearer " + firstLoginToken))
                .andExpect(status().isOk());

        // Verify directly against the DB: password hash actually changed and is BCrypt-encoded,
        // and mustChangePassword is now false.
        User updated = userRepository.findByEmailIgnoreCase("cpf-member@example.com").orElseThrow();
        assertFalse(updated.isMustChangePassword(), "mustChangePassword must be false after a successful change");
        assertTrue(updated.getPassword().startsWith("$2"), "password must be stored as a BCrypt hash");
        assertFalse(passwordEncoder.matches(tempPassword, updated.getPassword()), "old temp password must no longer match");
        assertTrue(passwordEncoder.matches(newPassword, updated.getPassword()), "new password must match the stored hash");

        // Old temporary password must be rejected
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("email", "cpf-member@example.com", "password", tempPassword))))
                .andExpect(status().isUnauthorized());

        // New password must work, and the account must no longer be flagged for a forced change
        MvcResult secondLoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("email", "cpf-member@example.com", "password", newPassword))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mustChangePassword").value(false))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();
        String secondLoginToken = JsonPath.read(secondLoginResult.getResponse().getContentAsString(), "$.data.token");

        // The freshly issued token must work against a protected endpoint
        mockMvc.perform(get("/api/members").header("Authorization", "Bearer " + secondLoginToken))
                .andExpect(status().isOk());
    }

    @Test
    void wrongCurrentPassword_returns400NotUnauthorized_andSessionSurvives() throws Exception {
        Map<String, String> registerBody = Map.of(
                "fullName", "Solo User", "email", "cpf-wrongcurrent@example.com",
                "password", "originalPass1", "mobileNumber", "9222222222"
        );
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json").content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isOk()).andReturn();
        String token = JsonPath.read(registerResult.getResponse().getContentAsString(), "$.data.token");
        mockMvc.perform(post("/api/rooms").header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("roomName", "Solo Room", "address", "1 Solo St"))))
                .andExpect(status().isOk());

        // Submitting the wrong "current password" must be a 400 (validation failure on an
        // already-authenticated request), never a 401 (which the frontend interceptor treats
        // as "your session expired" and would incorrectly log the user out).
        mockMvc.perform(post("/api/auth/change-password").header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("currentPassword", "wrongOne1", "newPassword", "somethingNew1"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Current password is incorrect"));

        // The token must still be valid afterward - a failed change attempt must not affect
        // the caller's existing session.
        mockMvc.perform(get("/api/members").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // The password itself must be unchanged by the failed attempt.
        User user = userRepository.findByEmailIgnoreCase("cpf-wrongcurrent@example.com").orElseThrow();
        assertTrue(passwordEncoder.matches("originalPass1", user.getPassword()));
    }
}

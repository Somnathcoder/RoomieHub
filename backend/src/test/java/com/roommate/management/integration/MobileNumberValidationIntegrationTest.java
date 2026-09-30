package com.roommate.management.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Mobile number must be mandatory + exactly 10 digits on both self-registration and admin add-member. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MobileNumberValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private Map<String, Object> registerBody(String email, String mobile) {
        Map<String, Object> body = new HashMap<>(Map.of(
                "fullName", "Mobile Test User", "email", email, "password", "password123"));
        if (mobile != null) body.put("mobileNumber", mobile);
        return body;
    }

    @Test
    void register_withMissingMobile_isRejected() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerBody("mv-missing@example.com", null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_withNonDigitMobile_isRejected() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerBody("mv-alpha@example.com", "98765abcde"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_withShortMobile_isRejected() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerBody("mv-short@example.com", "98765"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_withLongMobile_isRejected() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerBody("mv-long@example.com", "987654321099"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_withSpacesInMobile_isRejected() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerBody("mv-spaces@example.com", "98765 4321"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_withValidTenDigitMobile_succeeds() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerBody("mv-valid@example.com", "9876543210"))))
                .andExpect(status().isOk());
    }

    @Test
    void addMember_withMissingMobile_isRejected() throws Exception {
        MvcResult adminResult = mockMvc.perform(post("/api/auth/register").contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerBody("mv-admin@example.com", "9111122222"))))
                .andExpect(status().isOk()).andReturn();
        String adminToken = JsonPath.read(adminResult.getResponse().getContentAsString(), "$.data.token");

        mockMvc.perform(post("/api/rooms").header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("roomName", "Mobile Room", "address", "1 Mobile St"))))
                .andExpect(status().isOk());

        Map<String, Object> memberBodyNoMobile = Map.of(
                "fullName", "No Mobile Member", "email", "mv-nomobile@example.com", "role", "MEMBER");
        mockMvc.perform(post("/api/members").header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(memberBodyNoMobile)))
                .andExpect(status().isBadRequest());

        Map<String, Object> memberBodyBadMobile = Map.of(
                "fullName", "Bad Mobile Member", "email", "mv-badmobile@example.com", "role", "MEMBER",
                "mobileNumber", "12345");
        mockMvc.perform(post("/api/members").header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(memberBodyBadMobile)))
                .andExpect(status().isBadRequest());

        Map<String, Object> memberBodyValid = Map.of(
                "fullName", "Good Mobile Member", "email", "mv-goodmobile@example.com", "role", "MEMBER",
                "mobileNumber", "9876500000");
        mockMvc.perform(post("/api/members").header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(memberBodyValid)))
                .andExpect(status().isOk());
    }
}

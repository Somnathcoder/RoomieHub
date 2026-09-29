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

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Covers: bill creation, cleaning schedule creation, and permission
 * validation for a MODERATOR whose access is granted/denied per permission.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ModulePermissionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndLogin(String name, String email) throws Exception {
        Map<String, String> body = Map.of("fullName", name, "email", email, "password", "password123", "mobileNumber", "9555555555");
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json").content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }

    @Test
    void billCreation_andCleaningSchedule_workForAdmin() throws Exception {
        String adminToken = registerAndLogin("Bill Admin", "billadmin@example.com");
        mockMvc.perform(post("/api/rooms").header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("roomName", "Bill Room", "address", "Addr"))))
                .andExpect(status().isOk());

        Map<String, Object> billBody = Map.of(
                "title", "Electricity Bill", "amount", 1500, "dueDate", "2026-02-01", "category", "ELECTRICITY"
        );
        mockMvc.perform(post("/api/bills").header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json").content(objectMapper.writeValueAsString(billBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.amount").value(1500));

        MvcResult listResult = mockMvc.perform(get("/api/members").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        int adminMemberId = JsonPath.read(listResult.getResponse().getContentAsString(), "$.data[0].roomMemberId");

        Map<String, Object> cleaningBody = Map.of(
                "title", "Kitchen Cleaning", "cleaningDate", "2026-02-05",
                "assignedToMemberId", adminMemberId, "taskType", "KITCHEN"
        );
        mockMvc.perform(post("/api/cleaning-schedules").header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json").content(objectMapper.writeValueAsString(cleaningBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.taskType").value("KITCHEN"));
    }

    @Test
    void moderator_withoutGrantedPermission_isForbidden_thenAllowedAfterGrant() throws Exception {
        String adminToken = registerAndLogin("Perm Admin", "permadmin@example.com");
        String moderatorToken = registerAndLogin("Perm Moderator", "permmod@example.com");

        mockMvc.perform(post("/api/rooms").header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("roomName", "Perm Room", "address", "Addr"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/members").header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Perm Moderator", "email", "permmod@example.com",
                                "password", "password123", "role", "MODERATOR"))))
                .andExpect(status().isOk());

        MvcResult listResult = mockMvc.perform(get("/api/members").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        String listJson = listResult.getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(listJson, "$.data[*].roomMemberId");
        List<String> emails = JsonPath.read(listJson, "$.data[*].email");
        int moderatorMemberId = ids.get(emails.indexOf("permmod@example.com"));

        Map<String, Object> billBody = Map.of("title", "Water Bill", "amount", 500, "dueDate", "2026-02-01", "category", "WATER");

        // Moderator has no MANAGE_BILL permission yet -> forbidden
        mockMvc.perform(post("/api/bills").header("Authorization", "Bearer " + moderatorToken)
                        .contentType("application/json").content(objectMapper.writeValueAsString(billBody)))
                .andExpect(status().isForbidden());

        // Admin grants MANAGE_BILL permission to the moderator
        Map<String, Object> grantBody = Map.of("permissions", List.of(Map.of("code", "MANAGE_BILL", "granted", true)));
        mockMvc.perform(put("/api/members/" + moderatorMemberId + "/permissions").header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json").content(objectMapper.writeValueAsString(grantBody)))
                .andExpect(status().isOk());

        // Now the moderator can create a bill
        mockMvc.perform(post("/api/bills").header("Authorization", "Bearer " + moderatorToken)
                        .contentType("application/json").content(objectMapper.writeValueAsString(billBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Water Bill"));
    }
}

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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end flow: register admin + member, create a room, add the member,
 * create an equal-split expense as the member (goes to PENDING), approve it
 * as admin, and verify a settlement is generated with the correct amount.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ExpenseSettlementFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndLogin(String name, String email, String password) throws Exception {
        Map<String, String> body = Map.of("fullName", name, "email", email, "password", password);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json").content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }

    @Test
    void memberCreatedExpense_requiresApproval_thenGeneratesSettlement() throws Exception {
        String adminToken = registerAndLogin("Admin Roommate", "admin@example.com", "password123");
        String memberToken = registerAndLogin("Member Roommate", "member@example.com", "password123");

        // Admin creates a room and automatically becomes ADMIN
        Map<String, String> roomBody = Map.of("roomName", "Sunset Apartment", "address", "123 Main St");
        mockMvc.perform(post("/api/rooms").header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json").content(objectMapper.writeValueAsString(roomBody)))
                .andExpect(status().isOk());

        // Admin adds the second user as a MEMBER
        Map<String, Object> memberBody = Map.of(
                "fullName", "Member Roommate", "email", "member@example.com",
                "password", "password123", "role", "MEMBER"
        );
        mockMvc.perform(post("/api/members").header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json").content(objectMapper.writeValueAsString(memberBody)))
                .andExpect(status().isOk());

        // Fetch member IDs
        MvcResult listResult = mockMvc.perform(get("/api/members").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        String listJson = listResult.getResponse().getContentAsString();
        List<Integer> roomMemberIds = JsonPath.read(listJson, "$.data[*].roomMemberId");
        List<String> emails = JsonPath.read(listJson, "$.data[*].email");
        int adminMemberId = roomMemberIds.get(emails.indexOf("admin@example.com"));
        int memberMemberId = roomMemberIds.get(emails.indexOf("member@example.com"));

        // Member creates an equal-split expense of 1000, paid by admin, split between both
        Map<String, Object> expenseBody = Map.of(
                "title", "Groceries", "totalAmount", 1000, "category", "GROCERY",
                "paidByMemberId", adminMemberId, "expenseDate", "2026-01-15",
                "splitType", "EQUAL", "equalSplitMemberIds", List.of(adminMemberId, memberMemberId)
        );
        MvcResult createResult = mockMvc.perform(post("/api/expenses").header("Authorization", "Bearer " + memberToken)
                        .contentType("application/json").content(objectMapper.writeValueAsString(expenseBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING")) // member cannot self-approve
                .andReturn();
        int expenseId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.data.id");

        // Member cannot approve their own expense (no APPROVE_EXPENSE permission)
        Map<String, String> approveBody = Map.of("action", "APPROVE");
        mockMvc.perform(post("/api/expenses/" + expenseId + "/approve").header("Authorization", "Bearer " + memberToken)
                        .contentType("application/json").content(objectMapper.writeValueAsString(approveBody)))
                .andExpect(status().isForbidden());

        // Admin approves it
        mockMvc.perform(post("/api/expenses/" + expenseId + "/approve").header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json").content(objectMapper.writeValueAsString(approveBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        // A settlement should now exist: member owes admin 500
        MvcResult settlementResult = mockMvc.perform(get("/api/settlements").header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andReturn();
        List<Number> amounts = JsonPath.read(settlementResult.getResponse().getContentAsString(), "$.data[*].amount");
        assertEquals(1, amounts.size());
        assertEquals(500.0, amounts.get(0).doubleValue(), 0.01);
    }

    @Test
    void customSplit_sumMismatch_isRejected() throws Exception {
        String adminToken = registerAndLogin("Admin Two", "admin2@example.com", "password123");
        mockMvc.perform(post("/api/rooms").header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("roomName", "Room X", "address", "Addr X"))))
                .andExpect(status().isOk());

        MvcResult listResult = mockMvc.perform(get("/api/members").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        int adminMemberId = JsonPath.read(listResult.getResponse().getContentAsString(), "$.data[0].roomMemberId");

        Map<String, Object> badExpense = Map.of(
                "title", "Bad split", "totalAmount", 1000, "category", "OTHER",
                "paidByMemberId", adminMemberId, "expenseDate", "2026-01-15",
                "splitType", "CUSTOM",
                "customSplits", List.of(Map.of("roomMemberId", adminMemberId, "amount", 400))
        );
        mockMvc.perform(post("/api/expenses").header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json").content(objectMapper.writeValueAsString(badExpense)))
                .andExpect(status().isBadRequest());
    }
}

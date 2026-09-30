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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Covers the endpoints whose repository/service methods were just batched to remove N+1 query
 * patterns (expenses list, polls list, admin dashboard counts) - correctness matters more than
 * usual here since the batching logic (grouping splits/options/votes back onto their parent by
 * id) is new code, not just an annotation swap.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PerformanceRefactorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndLogin(String name, String email, String mobile) throws Exception {
        Map<String, String> body = Map.of("fullName", name, "email", email, "password", "password123", "mobileNumber", mobile);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json").content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }

    @Test
    void expensesList_returnsCorrectSplitsAndNames_forMultipleExpenses() throws Exception {
        String adminToken = registerAndLogin("Perf Admin", "perf-admin@example.com", "9111100001");
        mockMvc.perform(post("/api/rooms").header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("roomName", "Perf Room", "address", "1 Perf St"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/members").header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Perf Member", "email", "perf-member@example.com", "role", "MEMBER",
                                "mobileNumber", "9111100002"))))
                .andExpect(status().isOk());

        MvcResult listResult = mockMvc.perform(get("/api/members").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        String listJson = listResult.getResponse().getContentAsString();
        List<Integer> roomMemberIds = JsonPath.read(listJson, "$.data[*].roomMemberId");
        List<String> emails = JsonPath.read(listJson, "$.data[*].email");
        int adminId = roomMemberIds.get(emails.indexOf("perf-admin@example.com"));
        int memberId = roomMemberIds.get(emails.indexOf("perf-member@example.com"));

        // Two expenses, paid by different people, split equally between both members
        for (int i = 0; i < 2; i++) {
            Map<String, Object> expenseBody = Map.of(
                    "title", "Expense " + i, "totalAmount", 200, "category", "GROCERY",
                    "paidByMemberId", i == 0 ? adminId : memberId, "expenseDate", "2026-01-1" + i,
                    "splitType", "EQUAL", "equalSplitMemberIds", List.of(adminId, memberId));
            mockMvc.perform(post("/api/expenses").header("Authorization", "Bearer " + adminToken)
                            .contentType("application/json").content(objectMapper.writeValueAsString(expenseBody)))
                    .andExpect(status().isOk());
        }

        MvcResult expensesResult = mockMvc.perform(get("/api/expenses").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andReturn();
        String expensesJson = expensesResult.getResponse().getContentAsString();

        List<String> paidByNames = JsonPath.read(expensesJson, "$.data[*].paidByName");
        assertEquals(List.of("Perf Member", "Perf Admin"), paidByNames); // ordered by expense date desc

        // Each expense must have exactly 2 splits of 100 each, with correct member names attached
        List<List<Map<String, Object>>> allSplits = JsonPath.read(expensesJson, "$.data[*].splits");
        for (List<Map<String, Object>> splits : allSplits) {
            assertEquals(2, splits.size());
            List<String> memberNames = splits.stream().map(s -> (String) s.get("memberName")).sorted().toList();
            assertEquals(List.of("Perf Admin", "Perf Member"), memberNames);
        }
    }

    @Test
    void pollsList_returnsCorrectVoteCountsAndMyVote_forMultiplePolls() throws Exception {
        String adminToken = registerAndLogin("Poll Admin", "poll-admin@example.com", "9222200001");
        mockMvc.perform(post("/api/rooms").header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("roomName", "Poll Room", "address", "1 Poll St"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/members").header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Poll Member", "email", "poll-member@example.com", "role", "MEMBER",
                                "mobileNumber", "9222200002"))))
                .andExpect(status().isOk());

        // Create two polls with two options each
        MvcResult poll1Result = mockMvc.perform(post("/api/polls").header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("question", "Poll One", "options", List.of("A", "B")))))
                .andExpect(status().isOk()).andReturn();
        Long poll1Id = ((Number) JsonPath.read(poll1Result.getResponse().getContentAsString(), "$.data.id")).longValue();
        Long poll1OptionAId = ((Number) JsonPath.read(poll1Result.getResponse().getContentAsString(), "$.data.options[0].id")).longValue();

        mockMvc.perform(post("/api/polls").header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("question", "Poll Two", "options", List.of("X", "Y")))))
                .andExpect(status().isOk());

        // Admin votes on poll 1, option A
        mockMvc.perform(post("/api/polls/" + poll1Id + "/vote").header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("optionId", poll1OptionAId))))
                .andExpect(status().isOk());

        MvcResult listResult = mockMvc.perform(get("/api/polls").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andReturn();
        String json = listResult.getResponse().getContentAsString();

        // Poll Two was created after Poll One, so it's first (ordered by createdAt desc)
        assertEquals("Poll Two", JsonPath.read(json, "$.data[0].question"));
        assertEquals(0, ((Number) JsonPath.read(json, "$.data[0].totalVotes")).intValue());
        assertEquals("Poll One", JsonPath.read(json, "$.data[1].question"));
        assertEquals(1, ((Number) JsonPath.read(json, "$.data[1].totalVotes")).intValue());
        assertEquals(poll1OptionAId.intValue(), ((Number) JsonPath.read(json, "$.data[1].myVoteOptionId")).intValue());
        List<Integer> poll1VoteCounts = JsonPath.read(json, "$.data[1].options[*].voteCount");
        assertEquals(List.of(1, 0), poll1VoteCounts);
    }

    @Test
    void adminDashboard_countsAreCorrect_usingBatchedCountQueries() throws Exception {
        String adminToken = registerAndLogin("Dash Admin", "dash-admin@example.com", "9333300001");
        mockMvc.perform(post("/api/rooms").header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("roomName", "Dash Room", "address", "1 Dash St"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/members").header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Dash Member", "email", "dash-member@example.com", "role", "MEMBER",
                                "mobileNumber", "9333300002"))))
                .andExpect(status().isOk());

        MvcResult listResult = mockMvc.perform(get("/api/members").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        List<Integer> roomMemberIds = JsonPath.read(listResult.getResponse().getContentAsString(), "$.data[*].roomMemberId");
        int adminId = roomMemberIds.get(0);

        // One pending task, one already-completed task -> pendingTasks must be exactly 1
        mockMvc.perform(post("/api/tasks").header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("title", "Pending Task", "assignedToMemberId", adminId))))
                .andExpect(status().isOk());

        // One open issue -> openIssues must be exactly 1
        mockMvc.perform(post("/api/issues").header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("title", "Broken thing", "priority", "MEDIUM"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/dashboard/admin").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalMembers").value(2))
                .andExpect(jsonPath("$.data.activeMembers").value(2))
                .andExpect(jsonPath("$.data.pendingTasks").value(1))
                .andExpect(jsonPath("$.data.openIssues").value(1));
    }
}

package com.roommate.management.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Covers the ID-proof upload/view/authorization contract: correct Content-Type on view (the
 * bug that made the opened tab render blank), same-room access allowed, cross-room access
 * denied, and unauthenticated access rejected.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class IdProofAccessIntegrationTest {

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
    void idProof_uploadThenView_returnsCorrectContentType_andIsRoomIsolated() throws Exception {
        String adminToken = registerAndLogin("IdProof Admin", "idproof-admin@example.com", "9333333333");
        mockMvc.perform(post("/api/rooms").header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("roomName", "IdProof Room", "address", "1 Proof St"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/members").header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "IdProof Member", "email", "idproof-member@example.com", "role", "MEMBER",
                                "mobileNumber", "9555566666"))))
                .andExpect(status().isOk());

        MvcResult listResult = mockMvc.perform(get("/api/members").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        String listJson = listResult.getResponse().getContentAsString();
        List<Integer> roomMemberIds = JsonPath.read(listJson, "$.data[*].roomMemberId");
        List<String> emails = JsonPath.read(listJson, "$.data[*].email");
        int memberRoomMemberId = roomMemberIds.get(emails.indexOf("idproof-member@example.com"));

        // Admin uploads a PDF ID proof on the member's behalf
        MockMultipartFile file = new MockMultipartFile(
                "file", "aadhaar.pdf", "application/pdf", "fake-pdf-bytes".getBytes());
        mockMvc.perform(multipart("/api/members/" + memberRoomMemberId + "/id-proof")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Admin (same room) can view it, with the correct Content-Type - this is the exact bug
        // report: the tab opened but rendered blank because Content-Type was never set.
        mockMvc.perform(get("/api/members/" + memberRoomMemberId + "/id-proof").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().bytes("fake-pdf-bytes".getBytes()));

        // No token at all -> rejected
        mockMvc.perform(get("/api/members/" + memberRoomMemberId + "/id-proof"))
                .andExpect(status().isUnauthorized());

        // A user in a completely different room must not be able to view this member's ID proof
        String outsiderToken = registerAndLogin("Outsider Admin", "idproof-outsider@example.com", "9444444444");
        mockMvc.perform(post("/api/rooms").header("Authorization", "Bearer " + outsiderToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("roomName", "Other Room", "address", "2 Other St"))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/members/" + memberRoomMemberId + "/id-proof").header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());
    }
}

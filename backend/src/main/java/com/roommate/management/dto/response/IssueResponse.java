package com.roommate.management.dto.response;

import java.time.LocalDateTime;

public record IssueResponse(
        Long id,
        String title,
        String description,
        String photoUrl,
        String priority,
        String reportedByName,
        String status,
        LocalDateTime resolvedAt,
        LocalDateTime createdAt
) {}

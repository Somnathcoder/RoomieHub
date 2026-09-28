package com.roommate.management.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        String title,
        String description,
        Long assignedToMemberId,
        String assignedToName,
        LocalDate dueDate,
        String status,
        String priority,
        LocalDateTime completedAt
) {}

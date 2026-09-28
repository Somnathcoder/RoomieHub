package com.roommate.management.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record CleaningScheduleResponse(
        Long id,
        String title,
        LocalDate cleaningDate,
        LocalTime cleaningTime,
        Long assignedToMemberId,
        String assignedToName,
        String taskType,
        String description,
        String status,
        LocalDateTime completedAt
) {}

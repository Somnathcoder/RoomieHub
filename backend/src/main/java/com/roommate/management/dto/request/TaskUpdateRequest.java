package com.roommate.management.dto.request;

import com.roommate.management.entity.enums.TaskPriority;
import com.roommate.management.entity.enums.TaskStatus;

import java.time.LocalDate;

public record TaskUpdateRequest(
        String title,
        String description,
        Long assignedToMemberId,
        LocalDate dueDate,
        TaskPriority priority,
        TaskStatus status
) {}

package com.roommate.management.dto.request;

import com.roommate.management.entity.enums.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record TaskCreateRequest(
        @NotBlank String title,
        String description,
        @NotNull Long assignedToMemberId,
        LocalDate dueDate,
        TaskPriority priority
) {}

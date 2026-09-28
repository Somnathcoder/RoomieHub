package com.roommate.management.dto.request;

import com.roommate.management.entity.enums.CleaningTaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record CleaningScheduleCreateRequest(
        @NotBlank String title,
        @NotNull LocalDate cleaningDate,
        LocalTime cleaningTime,
        @NotNull Long assignedToMemberId,
        @NotNull CleaningTaskType taskType,
        String description
) {}

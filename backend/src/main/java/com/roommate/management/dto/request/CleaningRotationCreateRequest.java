package com.roommate.management.dto.request;

import com.roommate.management.entity.enums.CleaningTaskType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CleaningRotationCreateRequest(
        @NotNull CleaningTaskType taskType,
        @NotEmpty List<Long> memberIds,
        int intervalDays,
        @NotNull LocalDate startDate
) {}

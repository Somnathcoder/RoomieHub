package com.roommate.management.dto.request;

import com.roommate.management.entity.enums.CleaningStatus;
import com.roommate.management.entity.enums.CleaningTaskType;

import java.time.LocalDate;
import java.time.LocalTime;

public record CleaningScheduleUpdateRequest(
        String title,
        LocalDate cleaningDate,
        LocalTime cleaningTime,
        Long assignedToMemberId,
        CleaningTaskType taskType,
        String description,
        CleaningStatus status
) {}

package com.roommate.management.dto.response;

import java.time.LocalDate;
import java.util.List;

public record CleaningRotationResponse(
        Long id,
        String taskType,
        List<String> memberNames,
        List<Long> memberIds,
        int intervalDays,
        LocalDate startDate,
        boolean active,
        int currentIndex
) {}

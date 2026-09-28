package com.roommate.management.dto.request;

import java.util.List;

public record CleaningRotationUpdateRequest(
        List<Long> memberIds,
        Integer intervalDays,
        Boolean active
) {}

package com.roommate.management.dto.response;

import java.time.LocalDateTime;

public record ActivityLogResponse(
        Long id,
        String userName,
        String action,
        String module,
        Long referenceId,
        String description,
        LocalDateTime createdAt
) {}

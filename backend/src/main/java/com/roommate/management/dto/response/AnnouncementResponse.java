package com.roommate.management.dto.response;

import java.time.LocalDateTime;

public record AnnouncementResponse(
        Long id,
        String title,
        String message,
        String createdByName,
        String imageUrl,
        String status,
        LocalDateTime createdAt
) {}

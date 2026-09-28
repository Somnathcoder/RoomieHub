package com.roommate.management.dto.response;

import java.time.LocalDateTime;

public record RoomResponse(
        Long id,
        String roomName,
        String address,
        String createdByName,
        LocalDateTime createdAt,
        String status,
        long memberCount
) {}

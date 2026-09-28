package com.roommate.management.dto.response;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        Long senderId,
        String senderName,
        Long receiverId,
        String receiverName,
        String content,
        boolean isRead,
        String type,
        LocalDateTime createdAt
) {}

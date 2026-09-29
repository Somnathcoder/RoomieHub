package com.roommate.management.dto.response;

public record AuthResponse(
        String token,
        String tokenType,
        Long userId,
        String fullName,
        String email,
        Long roomId,
        String roomName,
        String role,
        boolean mustChangePassword
) {}

package com.roommate.management.dto.response;

public record PermissionResponse(
        String code,
        String description,
        boolean granted
) {}

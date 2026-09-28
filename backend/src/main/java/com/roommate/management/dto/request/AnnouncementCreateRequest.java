package com.roommate.management.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AnnouncementCreateRequest(
        @NotBlank String title,
        @NotBlank String message,
        String imageUrl
) {}

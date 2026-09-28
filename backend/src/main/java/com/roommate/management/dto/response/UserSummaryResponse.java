package com.roommate.management.dto.response;

public record UserSummaryResponse(
        Long id,
        String fullName,
        String email,
        String mobileNumber,
        String profilePhotoUrl
) {}

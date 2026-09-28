package com.roommate.management.dto.request;

public record MemberUpdateRequest(
        String fullName,
        String mobileNumber,
        String roomNumber,
        String bedNumber
) {}

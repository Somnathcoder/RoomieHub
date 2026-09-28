package com.roommate.management.dto.response;

import java.time.LocalDate;

public record MemberResponse(
        Long roomMemberId,
        Long userId,
        String fullName,
        String email,
        String mobileNumber,
        String profilePhotoUrl,
        boolean hasIdProof,
        String roomNumber,
        String bedNumber,
        LocalDate joiningDate,
        String status,
        String role,
        // Only populated once, in the response to the addMember call that created a brand-new
        // user account - this is the only place the admin can see the generated temporary
        // password, since it's stored encoded from then on. Null in every other response.
        String temporaryPassword
) {}

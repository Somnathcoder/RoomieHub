package com.roommate.management.dto.request;

import com.roommate.management.entity.enums.MemberStatus;
import jakarta.validation.constraints.NotNull;

public record MemberStatusUpdateRequest(
        @NotNull MemberStatus status
) {}

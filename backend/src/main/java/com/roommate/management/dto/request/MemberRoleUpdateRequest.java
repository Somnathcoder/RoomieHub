package com.roommate.management.dto.request;

import com.roommate.management.entity.enums.RoleType;
import jakarta.validation.constraints.NotNull;

public record MemberRoleUpdateRequest(
        @NotNull RoleType role
) {}

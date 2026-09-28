package com.roommate.management.dto.request;

import com.roommate.management.entity.enums.PermissionCode;
import jakarta.validation.constraints.NotNull;

public record PermissionItem(
        @NotNull PermissionCode code,
        boolean granted
) {}

package com.roommate.management.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record MemberPermissionsUpdateRequest(
        @NotEmpty List<PermissionItem> permissions
) {}

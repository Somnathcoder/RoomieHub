package com.roommate.management.dto.request;

import com.roommate.management.entity.enums.IssueStatus;
import jakarta.validation.constraints.NotNull;

public record IssueUpdateStatusRequest(
        @NotNull IssueStatus status
) {}

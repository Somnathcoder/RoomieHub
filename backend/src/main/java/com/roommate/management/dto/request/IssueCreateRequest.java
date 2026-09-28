package com.roommate.management.dto.request;

import com.roommate.management.entity.enums.IssuePriority;
import jakarta.validation.constraints.NotBlank;

public record IssueCreateRequest(
        @NotBlank String title,
        String description,
        IssuePriority priority
) {}

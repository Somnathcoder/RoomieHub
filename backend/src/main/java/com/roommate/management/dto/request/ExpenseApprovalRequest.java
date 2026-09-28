package com.roommate.management.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ExpenseApprovalRequest(
        @NotBlank String action, // APPROVE or REJECT
        String rejectionReason
) {}

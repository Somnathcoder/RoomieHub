package com.roommate.management.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ExpenseResponse(
        Long id,
        String title,
        String description,
        BigDecimal totalAmount,
        String category,
        Long paidByMemberId,
        String paidByName,
        LocalDate expenseDate,
        String receiptPhotoUrl,
        String splitType,
        String status,
        String createdByName,
        String approvedByName,
        LocalDateTime approvedAt,
        String rejectionReason,
        List<SplitResponse> splits
) {}

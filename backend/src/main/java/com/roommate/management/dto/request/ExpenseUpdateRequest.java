package com.roommate.management.dto.request;

import com.roommate.management.entity.enums.ExpenseCategory;
import com.roommate.management.entity.enums.SplitType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ExpenseUpdateRequest(
        String title,
        String description,
        BigDecimal totalAmount,
        ExpenseCategory category,
        Long paidByMemberId,
        LocalDate expenseDate,
        SplitType splitType,
        List<SplitItemRequest> customSplits,
        List<Long> equalSplitMemberIds
) {}

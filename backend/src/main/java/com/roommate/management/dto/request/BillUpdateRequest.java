package com.roommate.management.dto.request;

import com.roommate.management.entity.enums.BillCategory;
import com.roommate.management.entity.enums.BillStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BillUpdateRequest(
        String title,
        BigDecimal amount,
        BigDecimal amountPaid,
        LocalDate dueDate,
        BillCategory category,
        BillStatus status,
        Long paidByMemberId
) {}

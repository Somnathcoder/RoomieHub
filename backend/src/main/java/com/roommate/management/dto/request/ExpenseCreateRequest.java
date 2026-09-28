package com.roommate.management.dto.request;

import com.roommate.management.entity.enums.ExpenseCategory;
import com.roommate.management.entity.enums.SplitType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ExpenseCreateRequest(
        @NotBlank String title,
        String description,
        @NotNull @DecimalMin(value = "0.01") BigDecimal totalAmount,
        @NotNull ExpenseCategory category,
        @NotNull Long paidByMemberId,
        @NotNull LocalDate expenseDate,
        @NotNull SplitType splitType,
        List<SplitItemRequest> customSplits,
        List<Long> equalSplitMemberIds
) {}

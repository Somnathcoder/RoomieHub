package com.roommate.management.dto.request;

import com.roommate.management.entity.enums.ExpenseCategory;
import com.roommate.management.entity.enums.RecurrenceFrequency;
import com.roommate.management.entity.enums.SplitType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringExpenseCreateRequest(
        @NotNull String title,
        @NotNull ExpenseCategory category,
        @NotNull BigDecimal amount,
        @NotNull RecurrenceFrequency frequency,
        SplitType splitType,
        @NotNull LocalDate startDate,
        @NotNull Long paidByMemberId
) {}

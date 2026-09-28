package com.roommate.management.dto.request;

import com.roommate.management.entity.enums.BillCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BillCreateRequest(
        @NotBlank String title,
        @NotNull BigDecimal amount,
        @NotNull LocalDate dueDate,
        @NotNull BillCategory category,
        Long paidByMemberId
) {}

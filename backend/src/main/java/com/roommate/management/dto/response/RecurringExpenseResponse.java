package com.roommate.management.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringExpenseResponse(
        Long id,
        String title,
        String category,
        BigDecimal amount,
        String frequency,
        String splitType,
        LocalDate startDate,
        LocalDate nextDueDate,
        boolean active,
        String paidByName
) {}

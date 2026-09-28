package com.roommate.management.dto.request;

import com.roommate.management.entity.enums.RecurrenceFrequency;

import java.math.BigDecimal;

public record RecurringExpenseUpdateRequest(
        String title,
        BigDecimal amount,
        RecurrenceFrequency frequency,
        Boolean active
) {}

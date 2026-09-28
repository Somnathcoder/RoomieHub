package com.roommate.management.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BillResponse(
        Long id,
        String title,
        BigDecimal amount,
        BigDecimal amountPaid,
        LocalDate dueDate,
        String paidByName,
        String category,
        String billPhotoUrl,
        String status
) {}

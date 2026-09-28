package com.roommate.management.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SettlementResponse(
        Long id,
        Long fromMemberId,
        String fromMemberName,
        Long toMemberId,
        String toMemberName,
        BigDecimal amount,
        String status,
        LocalDate paymentDate,
        boolean verifiedByAdmin,
        String relatedExpenseTitle
) {}

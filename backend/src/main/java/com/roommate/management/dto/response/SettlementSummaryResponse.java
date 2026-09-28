package com.roommate.management.dto.response;

import java.math.BigDecimal;

public record SettlementSummaryResponse(
        Long fromMemberId,
        String fromMemberName,
        Long toMemberId,
        String toMemberName,
        BigDecimal netAmount
) {}

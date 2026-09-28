package com.roommate.management.dto.response;

import java.math.BigDecimal;

public record MemberBalanceResponse(
        Long roomMemberId,
        String memberName,
        BigDecimal totalPaid,
        BigDecimal totalShare,
        BigDecimal balance
) {}

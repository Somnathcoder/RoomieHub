package com.roommate.management.dto.response;

import java.math.BigDecimal;

public record SplitResponse(
        Long roomMemberId,
        String memberName,
        BigDecimal shareAmount
) {}

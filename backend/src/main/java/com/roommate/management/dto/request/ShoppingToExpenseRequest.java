package com.roommate.management.dto.request;

import com.roommate.management.entity.enums.SplitType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record ShoppingToExpenseRequest(
        @NotNull BigDecimal actualAmount,
        @NotNull Long paidByMemberId,
        @NotNull SplitType splitType,
        List<SplitItemRequest> customSplits,
        List<Long> equalSplitMemberIds
) {}

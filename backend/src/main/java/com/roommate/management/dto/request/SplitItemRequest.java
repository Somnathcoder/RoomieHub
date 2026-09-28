package com.roommate.management.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SplitItemRequest(
        @NotNull Long roomMemberId,
        @NotNull BigDecimal amount
) {}

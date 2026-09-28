package com.roommate.management.dto.response;

import java.math.BigDecimal;

public record NamedAmount(
        String label,
        BigDecimal amount
) {}

package com.roommate.management.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record ShoppingItemCreateRequest(
        @NotBlank String itemName,
        String quantity,
        BigDecimal estimatedAmount
) {}

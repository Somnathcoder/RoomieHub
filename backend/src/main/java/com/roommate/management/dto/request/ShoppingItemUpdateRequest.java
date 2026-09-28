package com.roommate.management.dto.request;

import com.roommate.management.entity.enums.ShoppingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ShoppingItemUpdateRequest(
        String itemName,
        String quantity,
        BigDecimal estimatedAmount,
        ShoppingStatus status,
        LocalDate purchaseDate
) {}

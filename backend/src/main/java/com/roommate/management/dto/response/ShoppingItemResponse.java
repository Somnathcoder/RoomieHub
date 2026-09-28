package com.roommate.management.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ShoppingItemResponse(
        Long id,
        String itemName,
        String quantity,
        BigDecimal estimatedAmount,
        String addedByName,
        String status,
        String itemPhotoUrl,
        String billPhotoUrl,
        LocalDate purchaseDate
) {}

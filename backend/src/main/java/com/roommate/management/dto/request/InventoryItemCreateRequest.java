package com.roommate.management.dto.request;

import com.roommate.management.entity.enums.InventoryCondition;
import jakarta.validation.constraints.NotBlank;

public record InventoryItemCreateRequest(
        @NotBlank String itemName,
        Integer quantity,
        InventoryCondition condition,
        String notes
) {}

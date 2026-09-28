package com.roommate.management.dto.request;

import com.roommate.management.entity.enums.InventoryCondition;

public record InventoryItemUpdateRequest(
        String itemName,
        Integer quantity,
        InventoryCondition condition,
        String notes
) {}

package com.roommate.management.dto.response;

import java.time.LocalDate;

public record InventoryItemResponse(
        Long id,
        String itemName,
        Integer quantity,
        String addedByName,
        LocalDate addedDate,
        String photoUrl,
        String condition,
        String notes
) {}

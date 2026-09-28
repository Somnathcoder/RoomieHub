package com.roommate.management.repository;

import com.roommate.management.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    List<InventoryItem> findByRoomIdOrderByCreatedAtDesc(Long roomId);
}

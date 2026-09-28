package com.roommate.management.repository;

import com.roommate.management.entity.ShoppingItem;
import com.roommate.management.entity.enums.ShoppingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShoppingItemRepository extends JpaRepository<ShoppingItem, Long> {
    List<ShoppingItem> findByRoomIdOrderByCreatedAtDesc(Long roomId);
    List<ShoppingItem> findByRoomIdAndStatus(Long roomId, ShoppingStatus status);
}

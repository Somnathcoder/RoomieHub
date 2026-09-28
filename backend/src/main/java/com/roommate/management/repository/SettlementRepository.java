package com.roommate.management.repository;

import com.roommate.management.entity.Settlement;
import com.roommate.management.entity.enums.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    List<Settlement> findByRoomId(Long roomId);
    List<Settlement> findByRoomIdAndStatus(Long roomId, SettlementStatus status);
    List<Settlement> findByFromMemberIdOrToMemberId(Long fromId, Long toId);
    List<Settlement> findByRelatedExpenseId(Long expenseId);
}

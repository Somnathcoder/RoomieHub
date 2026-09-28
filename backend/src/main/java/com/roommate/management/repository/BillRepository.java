package com.roommate.management.repository;

import com.roommate.management.entity.Bill;
import com.roommate.management.entity.enums.BillStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BillRepository extends JpaRepository<Bill, Long> {
    List<Bill> findByRoomIdOrderByDueDateAsc(Long roomId);
    List<Bill> findByRoomIdAndStatus(Long roomId, BillStatus status);
    List<Bill> findByStatusNotAndDueDateBefore(BillStatus status, LocalDate date);
    List<Bill> findByRoomIdAndDueDateBetween(Long roomId, LocalDate start, LocalDate end);
}

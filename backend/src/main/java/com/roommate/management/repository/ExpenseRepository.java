package com.roommate.management.repository;

import com.roommate.management.entity.Expense;
import com.roommate.management.entity.enums.ExpenseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByRoomIdOrderByExpenseDateDesc(Long roomId);
    List<Expense> findByRoomIdAndStatus(Long roomId, ExpenseStatus status);
    List<Expense> findByRoomIdAndExpenseDateBetween(Long roomId, LocalDate start, LocalDate end);
    List<Expense> findByPaidById(Long roomMemberId);
}

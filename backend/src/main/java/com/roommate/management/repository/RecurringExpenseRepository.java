package com.roommate.management.repository;

import com.roommate.management.entity.RecurringExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, Long> {
    List<RecurringExpense> findByRoomId(Long roomId);
    List<RecurringExpense> findByActiveTrueAndNextDueDateLessThanEqual(LocalDate date);
}

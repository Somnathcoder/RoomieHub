package com.roommate.management.repository;

import com.roommate.management.entity.ExpenseSplit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, Long> {
    List<ExpenseSplit> findByExpenseId(Long expenseId);
    List<ExpenseSplit> findByRoommateId(Long roomMemberId);
    void deleteByExpenseId(Long expenseId);
}

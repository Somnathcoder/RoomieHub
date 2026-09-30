package com.roommate.management.repository;

import com.roommate.management.entity.ExpenseSplit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, Long> {
    List<ExpenseSplit> findByExpenseId(Long expenseId);

    // Batch variant for listing N expenses at once - one query for all their splits (with the
    // member name already joined in) instead of one findByExpenseId call per expense.
    @EntityGraph(attributePaths = {"roommate.user"})
    List<ExpenseSplit> findByExpenseIdIn(List<Long> expenseIds);

    List<ExpenseSplit> findByRoommateId(Long roomMemberId);
    void deleteByExpenseId(Long expenseId);
}

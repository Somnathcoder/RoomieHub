package com.roommate.management.repository;

import com.roommate.management.entity.Expense;
import com.roommate.management.entity.enums.ExpenseStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    // These two back the main expenses-list endpoint. Without the entity graph, mapping each
    // row to a DTO walks paidBy/createdBy/approvedBy (each a lazy RoomMember -> lazy User) one
    // row at a time - up to 6 extra queries per expense. This fetches all of it in the same
    // query instead.
    @EntityGraph(attributePaths = {"paidBy.user", "createdBy.user", "approvedBy.user"})
    List<Expense> findByRoomIdOrderByExpenseDateDesc(Long roomId);

    @EntityGraph(attributePaths = {"paidBy.user", "createdBy.user", "approvedBy.user"})
    List<Expense> findByRoomIdAndStatus(Long roomId, ExpenseStatus status);

    List<Expense> findByRoomIdAndExpenseDateBetween(Long roomId, LocalDate start, LocalDate end);
    List<Expense> findByPaidById(Long roomMemberId);
}

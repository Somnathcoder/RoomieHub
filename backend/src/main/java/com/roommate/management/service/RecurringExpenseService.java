package com.roommate.management.service;

import com.roommate.management.dto.request.RecurringExpenseCreateRequest;
import com.roommate.management.dto.request.RecurringExpenseUpdateRequest;
import com.roommate.management.dto.response.RecurringExpenseResponse;
import com.roommate.management.entity.Expense;
import com.roommate.management.entity.RecurringExpense;
import com.roommate.management.entity.RoomMember;
import com.roommate.management.entity.enums.*;
import com.roommate.management.exception.ResourceNotFoundException;
import com.roommate.management.repository.ExpenseRepository;
import com.roommate.management.repository.ExpenseSplitRepository;
import com.roommate.management.repository.RecurringExpenseRepository;
import com.roommate.management.repository.RoomMemberRepository;
import com.roommate.management.util.SplitCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecurringExpenseService {

    private final RecurringExpenseRepository recurringExpenseRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final RoomAccessService roomAccessService;
    private final ActivityLogService activityLogService;
    private final SettlementService settlementService;

    @Transactional
    public RecurringExpenseResponse create(Long adminUserId, RecurringExpenseCreateRequest request) {
        RoomMember admin = roomAccessService.getActiveMembership(adminUserId);
        roomAccessService.requirePermission(admin, PermissionCode.MANAGE_EXPENSE);

        RoomMember paidBy = roomMemberRepository.findById(request.paidByMemberId())
                .orElseThrow(() -> new ResourceNotFoundException("Payer not found"));
        roomAccessService.requireSameRoom(admin, paidBy.getRoom().getId());

        RecurringExpense recurring = RecurringExpense.builder()
                .room(admin.getRoom())
                .title(request.title())
                .category(request.category())
                .amount(request.amount())
                .frequency(request.frequency())
                .splitType(request.splitType() == null ? SplitType.EQUAL : request.splitType())
                .startDate(request.startDate())
                .nextDueDate(request.startDate())
                .paidBy(paidBy)
                .createdBy(admin)
                .active(true)
                .build();
        recurring = recurringExpenseRepository.save(recurring);

        activityLogService.log(admin.getRoom(), admin.getUser(), ActivityModule.EXPENSE, "Recurring expense created", recurring.getId(),
                admin.getUser().getFullName() + " set up recurring expense \"" + recurring.getTitle() + "\"");

        return toResponse(recurring);
    }

    @Transactional(readOnly = true)
    public List<RecurringExpenseResponse> list(Long userId) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        return recurringExpenseRepository.findByRoomId(caller.getRoom().getId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public RecurringExpenseResponse update(Long adminUserId, Long id, RecurringExpenseUpdateRequest request) {
        RoomMember admin = roomAccessService.getActiveMembership(adminUserId);
        roomAccessService.requirePermission(admin, PermissionCode.MANAGE_EXPENSE);
        RecurringExpense recurring = recurringExpenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring expense not found"));
        roomAccessService.requireSameRoom(admin, recurring.getRoom().getId());

        if (request.title() != null) recurring.setTitle(request.title());
        if (request.amount() != null) recurring.setAmount(request.amount());
        if (request.frequency() != null) recurring.setFrequency(request.frequency());
        if (request.active() != null) recurring.setActive(request.active());

        return toResponse(recurringExpenseRepository.save(recurring));
    }

    @Transactional
    public void delete(Long adminUserId, Long id) {
        RoomMember admin = roomAccessService.getActiveMembership(adminUserId);
        roomAccessService.requirePermission(admin, PermissionCode.MANAGE_EXPENSE);
        RecurringExpense recurring = recurringExpenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring expense not found"));
        roomAccessService.requireSameRoom(admin, recurring.getRoom().getId());
        recurringExpenseRepository.delete(recurring);
    }

    /** Invoked by the scheduler. Generates a real Expense (auto-approved) for every recurring rule due today or earlier. */
    @Transactional
    public void generateDueExpenses(LocalDate today) {
        List<RecurringExpense> due = recurringExpenseRepository.findByActiveTrueAndNextDueDateLessThanEqual(today);
        for (RecurringExpense recurring : due) {
            Expense expense = Expense.builder()
                    .room(recurring.getRoom())
                    .title(recurring.getTitle() + " (Recurring)")
                    .description("Auto-generated recurring expense")
                    .totalAmount(recurring.getAmount())
                    .category(recurring.getCategory())
                    .paidBy(recurring.getPaidBy())
                    .expenseDate(recurring.getNextDueDate())
                    .splitType(recurring.getSplitType())
                    .createdBy(recurring.getCreatedBy())
                    .status(ExpenseStatus.APPROVED)
                    .approvedBy(recurring.getCreatedBy())
                    .approvedAt(java.time.LocalDateTime.now())
                    .recurringExpense(recurring)
                    .build();
            expense = expenseRepository.save(expense);

            List<Long> memberIds = roomMemberRepository.findByRoomIdAndStatus(recurring.getRoom().getId(), MemberStatus.ACTIVE)
                    .stream().map(RoomMember::getId).toList();
            Map<Long, BigDecimal> shares = SplitCalculator.equalSplit(expense.getTotalAmount(), memberIds);
            for (Map.Entry<Long, BigDecimal> entry : shares.entrySet()) {
                RoomMember member = roomMemberRepository.findById(entry.getKey()).orElseThrow();
                expenseSplitRepository.save(com.roommate.management.entity.ExpenseSplit.builder()
                        .expense(expense).roommate(member).shareAmount(entry.getValue()).build());
            }
            settlementService.generateForExpense(expense);

            recurring.setNextDueDate(advance(recurring.getNextDueDate(), recurring.getFrequency()));
            recurringExpenseRepository.save(recurring);

            activityLogService.log(recurring.getRoom(), recurring.getCreatedBy().getUser(), ActivityModule.EXPENSE,
                    "Recurring expense generated", expense.getId(),
                    "System generated expense \"" + expense.getTitle() + "\" from recurring rule");
        }
    }

    private LocalDate advance(LocalDate date, RecurrenceFrequency frequency) {
        return switch (frequency) {
            case DAILY -> date.plusDays(1);
            case WEEKLY -> date.plusWeeks(1);
            case MONTHLY -> date.plusMonths(1);
            case YEARLY -> date.plusYears(1);
        };
    }

    private RecurringExpenseResponse toResponse(RecurringExpense r) {
        return new RecurringExpenseResponse(
                r.getId(), r.getTitle(), r.getCategory().name(), r.getAmount(), r.getFrequency().name(),
                r.getSplitType().name(), r.getStartDate(), r.getNextDueDate(), r.isActive(), r.getPaidBy().getUser().getFullName()
        );
    }
}

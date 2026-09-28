package com.roommate.management.service;

import com.roommate.management.dto.request.ExpenseApprovalRequest;
import com.roommate.management.dto.request.ExpenseCreateRequest;
import com.roommate.management.dto.request.ExpenseUpdateRequest;
import com.roommate.management.dto.request.SplitItemRequest;
import com.roommate.management.dto.response.ExpenseResponse;
import com.roommate.management.dto.response.SplitResponse;
import com.roommate.management.entity.Expense;
import com.roommate.management.entity.ExpenseSplit;
import com.roommate.management.entity.RoomMember;
import com.roommate.management.entity.enums.*;
import com.roommate.management.exception.BadRequestException;
import com.roommate.management.exception.ForbiddenException;
import com.roommate.management.exception.ResourceNotFoundException;
import com.roommate.management.repository.ExpenseRepository;
import com.roommate.management.repository.ExpenseSplitRepository;
import com.roommate.management.repository.RoomMemberRepository;
import com.roommate.management.util.SplitCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomAccessService roomAccessService;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;
    private final SettlementService settlementService;
    private final FileStorageService fileStorageService;

    @Transactional
    public ExpenseResponse createExpense(Long userId, ExpenseCreateRequest request) {
        RoomMember creator = roomAccessService.getActiveMembership(userId);
        RoomMember paidBy = roomMemberRepository.findById(request.paidByMemberId())
                .orElseThrow(() -> new ResourceNotFoundException("Payer not found"));
        roomAccessService.requireSameRoom(creator, paidBy.getRoom().getId());

        Expense expense = Expense.builder()
                .room(creator.getRoom())
                .title(request.title())
                .description(request.description())
                .totalAmount(request.totalAmount())
                .category(request.category())
                .paidBy(paidBy)
                .expenseDate(request.expenseDate())
                .splitType(request.splitType())
                .createdBy(creator)
                .status(ExpenseStatus.PENDING)
                .build();
        expense = expenseRepository.save(expense);

        saveSplits(expense, request.splitType(), request.customSplits(), request.equalSplitMemberIds());

        boolean autoApprove = creator.getRole() == RoleType.ADMIN
                || roomAccessService.hasPermission(creator, PermissionCode.APPROVE_EXPENSE);

        activityLogService.log(creator.getRoom(), creator.getUser(), ActivityModule.EXPENSE, "Expense added", expense.getId(),
                creator.getUser().getFullName() + " added expense \"" + expense.getTitle() + "\" for " + expense.getTotalAmount());

        if (autoApprove) {
            approveInternal(expense, creator);
        } else {
            notifyAdmins(expense, "New expense pending approval",
                    creator.getUser().getFullName() + " submitted \"" + expense.getTitle() + "\" for approval.");
        }

        return toResponse(expenseRepository.findById(expense.getId()).orElseThrow());
    }

    @Transactional
    public ExpenseResponse updateExpense(Long userId, Long expenseId, ExpenseUpdateRequest request) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        roomAccessService.requireSameRoom(caller, expense.getRoom().getId());

        boolean isCreator = expense.getCreatedBy().getId().equals(caller.getId());
        boolean canManage = caller.getRole() == RoleType.ADMIN || roomAccessService.hasPermission(caller, PermissionCode.MANAGE_EXPENSE);
        if (!isCreator && !canManage) {
            throw new ForbiddenException("You cannot edit this expense");
        }
        if (expense.getStatus() != ExpenseStatus.PENDING && !canManage) {
            throw new ForbiddenException("Only an admin/moderator can edit an already-approved expense");
        }

        if (request.title() != null) expense.setTitle(request.title());
        if (request.description() != null) expense.setDescription(request.description());
        if (request.category() != null) expense.setCategory(request.category());
        if (request.expenseDate() != null) expense.setExpenseDate(request.expenseDate());
        if (request.totalAmount() != null) expense.setTotalAmount(request.totalAmount());
        if (request.paidByMemberId() != null) {
            RoomMember paidBy = roomMemberRepository.findById(request.paidByMemberId())
                    .orElseThrow(() -> new ResourceNotFoundException("Payer not found"));
            roomAccessService.requireSameRoom(caller, paidBy.getRoom().getId());
            expense.setPaidBy(paidBy);
        }
        if (request.splitType() != null) expense.setSplitType(request.splitType());

        boolean wasApproved = expense.getStatus() == ExpenseStatus.APPROVED;
        expense = expenseRepository.save(expense);

        if (request.customSplits() != null || request.equalSplitMemberIds() != null || request.totalAmount() != null || request.splitType() != null) {
            // Preserve the original split participants when the caller didn't explicitly resend
            // equalSplitMemberIds (e.g. only totalAmount changed). Without this, saveSplits()
            // would fall back to "all active room members", silently expanding who's included.
            List<Long> existingMemberIds = expenseSplitRepository.findByExpenseId(expense.getId()).stream()
                    .map(s -> s.getRoommate().getId()).toList();
            List<Long> equalMemberIds = request.equalSplitMemberIds() != null ? request.equalSplitMemberIds() : existingMemberIds;
            expenseSplitRepository.deleteByExpenseId(expense.getId());
            saveSplits(expense, expense.getSplitType(), request.customSplits(), equalMemberIds);
            if (wasApproved) {
                settlementService.regenerateForExpense(expense);
            }
        }

        activityLogService.log(caller.getRoom(), caller.getUser(), ActivityModule.EXPENSE, "Expense updated", expense.getId(),
                caller.getUser().getFullName() + " updated expense \"" + expense.getTitle() + "\"");

        return toResponse(expenseRepository.findById(expense.getId()).orElseThrow());
    }

    @Transactional
    public ExpenseResponse decideExpense(Long adminUserId, Long expenseId, ExpenseApprovalRequest request) {
        RoomMember admin = roomAccessService.getActiveMembership(adminUserId);
        roomAccessService.requirePermission(admin, PermissionCode.APPROVE_EXPENSE);

        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        roomAccessService.requireSameRoom(admin, expense.getRoom().getId());

        if (expense.getStatus() != ExpenseStatus.PENDING) {
            throw new BadRequestException("Only a pending expense can be approved or rejected");
        }

        if ("APPROVE".equalsIgnoreCase(request.action())) {
            approveInternal(expense, admin);
        } else if ("REJECT".equalsIgnoreCase(request.action())) {
            expense.setStatus(ExpenseStatus.REJECTED);
            expense.setApprovedBy(admin);
            expense.setApprovedAt(LocalDateTime.now());
            expense.setRejectionReason(request.rejectionReason());
            expenseRepository.save(expense);
            notificationService.notify(expense.getCreatedBy().getUser(), expense.getRoom(), NotificationType.EXPENSE_REJECTED,
                    "Expense rejected", "Your expense \"" + expense.getTitle() + "\" was rejected" +
                    (request.rejectionReason() != null ? ": " + request.rejectionReason() : "."), expense.getId());
            activityLogService.log(admin.getRoom(), admin.getUser(), ActivityModule.EXPENSE, "Expense rejected", expense.getId(),
                    admin.getUser().getFullName() + " rejected expense \"" + expense.getTitle() + "\"");
        } else {
            throw new BadRequestException("Action must be APPROVE or REJECT");
        }

        return toResponse(expenseRepository.findById(expense.getId()).orElseThrow());
    }

    private void approveInternal(Expense expense, RoomMember approver) {
        expense.setStatus(ExpenseStatus.APPROVED);
        expense.setApprovedBy(approver);
        expense.setApprovedAt(LocalDateTime.now());
        expenseRepository.save(expense);

        settlementService.generateForExpense(expense);

        notificationService.notify(expense.getCreatedBy().getUser(), expense.getRoom(), NotificationType.EXPENSE_APPROVED,
                "Expense approved", "Your expense \"" + expense.getTitle() + "\" was approved.", expense.getId());
        activityLogService.log(expense.getRoom(), approver.getUser(), ActivityModule.EXPENSE, "Expense approved", expense.getId(),
                approver.getUser().getFullName() + " approved expense \"" + expense.getTitle() + "\"");
    }

    @Transactional
    public void deleteExpense(Long userId, Long expenseId) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        roomAccessService.requireSameRoom(caller, expense.getRoom().getId());

        boolean isCreator = expense.getCreatedBy().getId().equals(caller.getId());
        boolean canManage = caller.getRole() == RoleType.ADMIN || roomAccessService.hasPermission(caller, PermissionCode.MANAGE_EXPENSE);
        if (!isCreator && !canManage) {
            throw new ForbiddenException("You cannot delete this expense");
        }
        settlementService.deleteForExpense(expense.getId());
        expenseSplitRepository.deleteByExpenseId(expense.getId());
        expenseRepository.delete(expense);

        activityLogService.log(caller.getRoom(), caller.getUser(), ActivityModule.EXPENSE, "Expense deleted", expenseId,
                caller.getUser().getFullName() + " deleted expense \"" + expense.getTitle() + "\"");
    }

    @Transactional
    public String uploadReceipt(Long userId, Long expenseId, MultipartFile file) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        roomAccessService.requireSameRoom(caller, expense.getRoom().getId());
        String path = fileStorageService.store(file, "receipts", List.of("image/jpeg", "image/png", "image/jpg", "application/pdf"));
        expense.setReceiptPhotoUrl(fileStorageService.toPublicUrl(path));
        expenseRepository.save(expense);
        return expense.getReceiptPhotoUrl();
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> listExpenses(Long userId, ExpenseStatus status) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        List<Expense> expenses = status == null
                ? expenseRepository.findByRoomIdOrderByExpenseDateDesc(caller.getRoom().getId())
                : expenseRepository.findByRoomIdAndStatus(caller.getRoom().getId(), status);
        return expenses.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getExpense(Long userId, Long expenseId) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        roomAccessService.requireSameRoom(caller, expense.getRoom().getId());
        return toResponse(expense);
    }

    private void saveSplits(Expense expense, SplitType splitType, List<SplitItemRequest> customSplits, List<Long> equalMemberIds) {
        Map<Long, BigDecimal> shares;
        if (splitType == SplitType.EQUAL) {
            List<Long> memberIds = (equalMemberIds == null || equalMemberIds.isEmpty())
                    ? roomMemberRepository.findByRoomIdAndStatus(expense.getRoom().getId(), MemberStatus.ACTIVE)
                        .stream().map(RoomMember::getId).toList()
                    : equalMemberIds;
            shares = SplitCalculator.equalSplit(expense.getTotalAmount(), memberIds);
        } else {
            shares = SplitCalculator.customSplit(expense.getTotalAmount(), customSplits);
        }

        for (Map.Entry<Long, BigDecimal> entry : shares.entrySet()) {
            RoomMember member = roomMemberRepository.findById(entry.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("Split member not found: " + entry.getKey()));
            roomAccessService.requireSameRoom(member, expense.getRoom().getId());
            ExpenseSplit split = ExpenseSplit.builder()
                    .expense(expense)
                    .roommate(member)
                    .shareAmount(entry.getValue())
                    .build();
            expenseSplitRepository.save(split);
        }
    }

    private void notifyAdmins(Expense expense, String title, String message) {
        roomMemberRepository.findByRoomIdAndStatus(expense.getRoom().getId(), MemberStatus.ACTIVE).stream()
                .filter(m -> m.getRole() == RoleType.ADMIN)
                .forEach(admin -> notificationService.notify(admin.getUser(), expense.getRoom(),
                        NotificationType.NEW_EXPENSE, title, message, expense.getId()));
    }

    ExpenseResponse toResponse(Expense e) {
        List<SplitResponse> splits = expenseSplitRepository.findByExpenseId(e.getId()).stream()
                .map(s -> new SplitResponse(s.getRoommate().getId(), s.getRoommate().getUser().getFullName(), s.getShareAmount()))
                .toList();
        return new ExpenseResponse(
                e.getId(), e.getTitle(), e.getDescription(), e.getTotalAmount(), e.getCategory().name(),
                e.getPaidBy().getId(), e.getPaidBy().getUser().getFullName(), e.getExpenseDate(), e.getReceiptPhotoUrl(),
                e.getSplitType().name(), e.getStatus().name(), e.getCreatedBy().getUser().getFullName(),
                e.getApprovedBy() != null ? e.getApprovedBy().getUser().getFullName() : null,
                e.getApprovedAt(), e.getRejectionReason(), splits
        );
    }
}

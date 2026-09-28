package com.roommate.management.service;

import com.roommate.management.dto.request.SettlementPayRequest;
import com.roommate.management.dto.response.MemberBalanceResponse;
import com.roommate.management.dto.response.SettlementResponse;
import com.roommate.management.dto.response.SettlementSummaryResponse;
import com.roommate.management.entity.Expense;
import com.roommate.management.entity.ExpenseSplit;
import com.roommate.management.entity.RoomMember;
import com.roommate.management.entity.Settlement;
import com.roommate.management.entity.enums.*;
import com.roommate.management.exception.BadRequestException;
import com.roommate.management.exception.ForbiddenException;
import com.roommate.management.exception.ResourceNotFoundException;
import com.roommate.management.repository.ExpenseRepository;
import com.roommate.management.repository.ExpenseSplitRepository;
import com.roommate.management.repository.RoomMemberRepository;
import com.roommate.management.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final ExpenseRepository expenseRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomAccessService roomAccessService;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;

    @Transactional
    public void generateForExpense(Expense expense) {
        List<ExpenseSplit> splits = expenseSplitRepository.findByExpenseId(expense.getId());
        for (ExpenseSplit split : splits) {
            if (split.getRoommate().getId().equals(expense.getPaidBy().getId())) {
                continue; // payer doesn't owe themselves
            }
            Settlement settlement = Settlement.builder()
                    .room(expense.getRoom())
                    .relatedExpense(expense)
                    .fromMember(split.getRoommate())
                    .toMember(expense.getPaidBy())
                    .amount(split.getShareAmount())
                    .status(SettlementStatus.PENDING)
                    .build();
            settlementRepository.save(settlement);
            notificationService.notify(split.getRoommate().getUser(), expense.getRoom(), NotificationType.PAYMENT_REMINDER,
                    "You owe " + expense.getPaidBy().getUser().getFullName(),
                    "Your share of \"" + expense.getTitle() + "\" is " + split.getShareAmount() + ".", expense.getId());
        }
    }

    @Transactional
    public void regenerateForExpense(Expense expense) {
        deleteForExpense(expense.getId());
        generateForExpense(expense);
    }

    @Transactional
    public void deleteForExpense(Long expenseId) {
        settlementRepository.deleteAll(settlementRepository.findByRelatedExpenseId(expenseId));
    }

    @Transactional(readOnly = true)
    public List<SettlementResponse> listSettlements(Long userId, SettlementStatus status) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        List<Settlement> settlements = status == null
                ? settlementRepository.findByRoomId(caller.getRoom().getId())
                : settlementRepository.findByRoomIdAndStatus(caller.getRoom().getId(), status);
        return settlements.stream().map(this::toResponse).toList();
    }

    @Transactional
    public SettlementResponse pay(Long userId, Long settlementId, SettlementPayRequest request) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement not found"));
        roomAccessService.requireSameRoom(caller, settlement.getRoom().getId());

        boolean isPayer = settlement.getFromMember().getId().equals(caller.getId());
        boolean isAdmin = caller.getRole() == RoleType.ADMIN;
        if (!isPayer && !isAdmin) {
            throw new ForbiddenException("Only the person who owes this amount (or the admin) can mark it as paid");
        }
        if (settlement.getStatus() == SettlementStatus.PAID) {
            throw new BadRequestException("This settlement is already marked as paid");
        }

        settlement.setStatus(SettlementStatus.PAID);
        settlement.setPaymentDate(request.paymentDate() == null ? LocalDate.now() : request.paymentDate());
        if (isAdmin) {
            settlement.setVerifiedByAdmin(true);
            settlement.setVerifiedBy(caller);
        }
        settlement = settlementRepository.save(settlement);

        activityLogService.log(caller.getRoom(), caller.getUser(), ActivityModule.SETTLEMENT, "Settlement paid", settlement.getId(),
                settlement.getFromMember().getUser().getFullName() + " paid " + settlement.getAmount() + " to " + settlement.getToMember().getUser().getFullName());
        notificationService.notify(settlement.getToMember().getUser(), caller.getRoom(), NotificationType.PAYMENT_REMINDER,
                "Payment received", settlement.getFromMember().getUser().getFullName() + " marked " + settlement.getAmount() + " as paid.",
                settlement.getId());

        return toResponse(settlement);
    }

    @Transactional
    public SettlementResponse verify(Long adminUserId, Long settlementId) {
        RoomMember admin = roomAccessService.getActiveMembership(adminUserId);
        roomAccessService.requireAdmin(admin);
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement not found"));
        roomAccessService.requireSameRoom(admin, settlement.getRoom().getId());
        settlement.setVerifiedByAdmin(true);
        settlement.setVerifiedBy(admin);
        return toResponse(settlementRepository.save(settlement));
    }

    @Transactional(readOnly = true)
    public List<MemberBalanceResponse> getBalances(Long userId) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        Long roomId = caller.getRoom().getId();
        List<RoomMember> members = roomMemberRepository.findByRoomIdAndStatus(roomId, MemberStatus.ACTIVE);

        Map<Long, BigDecimal> paid = new HashMap<>();
        Map<Long, BigDecimal> share = new HashMap<>();
        for (RoomMember m : members) {
            paid.put(m.getId(), BigDecimal.ZERO);
            share.put(m.getId(), BigDecimal.ZERO);
        }

        List<Expense> approvedExpenses = expenseRepository.findByRoomIdAndStatus(roomId, ExpenseStatus.APPROVED);
        for (Expense e : approvedExpenses) {
            paid.merge(e.getPaidBy().getId(), e.getTotalAmount(), BigDecimal::add);
            for (ExpenseSplit split : expenseSplitRepository.findByExpenseId(e.getId())) {
                share.merge(split.getRoommate().getId(), split.getShareAmount(), BigDecimal::add);
            }
        }

        List<MemberBalanceResponse> result = new ArrayList<>();
        for (RoomMember m : members) {
            BigDecimal p = paid.getOrDefault(m.getId(), BigDecimal.ZERO);
            BigDecimal s = share.getOrDefault(m.getId(), BigDecimal.ZERO);
            result.add(new MemberBalanceResponse(m.getId(), m.getUser().getFullName(), p, s, p.subtract(s)));
        }
        return result;
    }

    /** Nets all pending settlements between each pair of members down to a single owed amount. */
    @Transactional(readOnly = true)
    public List<SettlementSummaryResponse> getSummary(Long userId) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        List<Settlement> pending = settlementRepository.findByRoomIdAndStatus(caller.getRoom().getId(), SettlementStatus.PENDING);

        Map<String, BigDecimal> net = new LinkedHashMap<>();
        Map<String, RoomMember[]> pairs = new LinkedHashMap<>();
        for (Settlement s : pending) {
            Long a = s.getFromMember().getId();
            Long b = s.getToMember().getId();
            String forwardKey = a + "->" + b;
            String reverseKey = b + "->" + a;
            if (net.containsKey(reverseKey)) {
                net.merge(reverseKey, s.getAmount().negate(), BigDecimal::add);
            } else {
                net.merge(forwardKey, s.getAmount(), BigDecimal::add);
                pairs.put(forwardKey, new RoomMember[]{s.getFromMember(), s.getToMember()});
            }
        }

        List<SettlementSummaryResponse> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : net.entrySet()) {
            RoomMember[] pair = pairs.get(entry.getKey());
            if (pair == null) continue;
            BigDecimal amount = entry.getValue();
            if (amount.compareTo(BigDecimal.ZERO) == 0) continue;
            RoomMember from = amount.compareTo(BigDecimal.ZERO) > 0 ? pair[0] : pair[1];
            RoomMember to = amount.compareTo(BigDecimal.ZERO) > 0 ? pair[1] : pair[0];
            result.add(new SettlementSummaryResponse(from.getId(), from.getUser().getFullName(),
                    to.getId(), to.getUser().getFullName(), amount.abs()));
        }
        return result;
    }

    private SettlementResponse toResponse(Settlement s) {
        return new SettlementResponse(
                s.getId(), s.getFromMember().getId(), s.getFromMember().getUser().getFullName(),
                s.getToMember().getId(), s.getToMember().getUser().getFullName(), s.getAmount(),
                s.getStatus().name(), s.getPaymentDate(), s.isVerifiedByAdmin(),
                s.getRelatedExpense() != null ? s.getRelatedExpense().getTitle() : null
        );
    }
}

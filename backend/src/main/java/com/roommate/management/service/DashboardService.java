package com.roommate.management.service;

import com.roommate.management.dto.response.*;
import com.roommate.management.entity.*;
import com.roommate.management.entity.enums.*;
import com.roommate.management.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final RoomAccessService roomAccessService;
    private final RoomMemberRepository roomMemberRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final SettlementRepository settlementRepository;
    private final BillRepository billRepository;
    private final TaskRepository taskRepository;
    private final CleaningScheduleRepository cleaningScheduleRepository;
    private final MessageRepository messageRepository;
    private final NotificationRepository notificationRepository;
    private final AnnouncementRepository announcementRepository;
    private final IssueRepository issueRepository;
    private final ActivityLogRepository activityLogRepository;

    private final ExpenseService expenseService;

    @Transactional(readOnly = true)
    public MemberDashboardResponse getMemberDashboard(Long userId) {
        RoomMember member = roomAccessService.getActiveMembership(userId);
        Long roomId = member.getRoom().getId();
        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        LocalDate monthEnd = now.withDayOfMonth(now.lengthOfMonth());

        List<Expense> monthlyApproved = expenseRepository.findByRoomIdAndExpenseDateBetween(roomId, monthStart, monthEnd).stream()
                .filter(e -> e.getStatus() == ExpenseStatus.APPROVED).toList();
        BigDecimal totalMonthly = monthlyApproved.stream().map(Expense::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal myContribution = monthlyApproved.stream()
                .filter(e -> e.getPaidBy().getId().equals(member.getId()))
                .map(Expense::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal myPending = settlementRepository.findByRoomIdAndStatus(roomId, SettlementStatus.PENDING).stream()
                .filter(s -> s.getFromMember().getId().equals(member.getId()))
                .map(Settlement::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<BillResponse> upcomingBills = billRepository.findByRoomIdAndDueDateBetween(roomId, now, now.plusDays(7)).stream()
                .filter(b -> b.getStatus() != BillStatus.PAID)
                .map(this::toBillResponse).toList();

        List<TaskResponse> myPendingTasks = taskRepository.findByAssignedToIdOrderByDueDateAsc(member.getId()).stream()
                .filter(t -> t.getStatus() != TaskStatus.COMPLETED)
                .map(this::toTaskResponse).toList();

        LocalDate nextCleaning = cleaningScheduleRepository.findByAssignedToIdOrderByCleaningDateAsc(member.getId()).stream()
                .filter(c -> c.getStatus() != CleaningStatus.COMPLETED && !c.getCleaningDate().isBefore(now))
                .map(CleaningSchedule::getCleaningDate).findFirst().orElse(null);

        long unreadMessages = messageRepository.countByReceiverIdAndIsReadFalse(userId);
        long unreadNotifications = notificationRepository.countByUserIdAndIsReadFalse(userId);

        List<ExpenseResponse> recentExpenses = expenseRepository.findByRoomIdOrderByExpenseDateDesc(roomId).stream()
                .limit(5).map(expenseService::toResponse).toList();

        List<AnnouncementResponse> recentAnnouncements = announcementRepository.findByRoomIdOrderByCreatedAtDesc(roomId).stream()
                .filter(a -> a.getStatus() == AnnouncementStatus.ACTIVE)
                .limit(5)
                .map(a -> new AnnouncementResponse(a.getId(), a.getTitle(), a.getMessage(), a.getCreatedBy().getUser().getFullName(),
                        a.getImageUrl(), a.getStatus().name(), a.getCreatedAt()))
                .toList();

        return new MemberDashboardResponse(totalMonthly, myContribution, myPending, upcomingBills, myPendingTasks,
                nextCleaning, unreadMessages, unreadNotifications, recentExpenses, recentAnnouncements);
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getAdminDashboard(Long userId) {
        RoomMember admin = roomAccessService.getActiveMembership(userId);
        roomAccessService.requireAdmin(admin);
        Long roomId = admin.getRoom().getId();
        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        LocalDate monthEnd = now.withDayOfMonth(now.lengthOfMonth());

        long totalMembers = roomMemberRepository.countByRoomId(roomId);
        long activeMembers = roomMemberRepository.countByRoomIdAndStatus(roomId, MemberStatus.ACTIVE);

        BigDecimal monthlyExpenses = expenseRepository.findByRoomIdAndExpenseDateBetween(roomId, monthStart, monthEnd).stream()
                .filter(e -> e.getStatus() == ExpenseStatus.APPROVED)
                .map(Expense::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pendingPayments = settlementRepository.findByRoomIdAndStatus(roomId, SettlementStatus.PENDING).stream()
                .map(Settlement::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingExpenses = expenseRepository.findByRoomIdAndStatus(roomId, ExpenseStatus.PENDING).size();
        long pendingTasks = taskRepository.countByRoomIdAndStatusNot(roomId, TaskStatus.COMPLETED);

        List<BillResponse> upcomingBills = billRepository.findByRoomIdAndDueDateBetween(roomId, now, now.plusDays(7)).stream()
                .filter(b -> b.getStatus() != BillStatus.PAID).map(this::toBillResponse).toList();

        List<CleaningScheduleResponse> upcomingCleaning = cleaningScheduleRepository.findByRoomIdAndCleaningDateBetween(roomId, now, now.plusDays(7))
                .stream().map(this::toCleaningResponse).toList();

        List<ActivityLogResponse> recentActivities = activityLogRepository.findByRoomIdOrderByCreatedAtDesc(roomId).stream()
                .limit(10)
                .map(a -> new ActivityLogResponse(a.getId(), a.getUser().getFullName(), a.getAction(), a.getModule().name(),
                        a.getReferenceId(), a.getDescription(), a.getCreatedAt()))
                .toList();

        long openIssues = issueRepository.countByRoomIdAndStatusNot(roomId, IssueStatus.RESOLVED);

        return new AdminDashboardResponse(totalMembers, activeMembers, monthlyExpenses, pendingPayments, pendingExpenses,
                pendingTasks, upcomingBills, upcomingCleaning, recentActivities, openIssues);
    }

    @Transactional(readOnly = true)
    public ExpenseAnalyticsResponse getAnalytics(Long userId) {
        RoomMember member = roomAccessService.getActiveMembership(userId);
        Long roomId = member.getRoom().getId();
        List<Expense> approved = expenseRepository.findByRoomIdOrderByExpenseDateDesc(roomId).stream()
                .filter(e -> e.getStatus() == ExpenseStatus.APPROVED).toList();

        // last 6 months
        Map<String, BigDecimal> monthly = new LinkedHashMap<>();
        YearMonth cursor = YearMonth.now().minusMonths(5);
        for (int i = 0; i < 6; i++) {
            monthly.put(cursor.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + cursor.getYear(), BigDecimal.ZERO);
            cursor = cursor.plusMonths(1);
        }
        for (Expense e : approved) {
            String key = e.getExpenseDate().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + e.getExpenseDate().getYear();
            if (monthly.containsKey(key)) {
                monthly.merge(key, e.getTotalAmount(), BigDecimal::add);
            }
        }

        Map<String, BigDecimal> category = new LinkedHashMap<>();
        for (Expense e : approved) {
            category.merge(e.getCategory().name(), e.getTotalAmount(), BigDecimal::add);
        }

        Map<String, BigDecimal> contribution = new LinkedHashMap<>();
        for (Expense e : approved) {
            contribution.merge(e.getPaidBy().getUser().getFullName(), e.getTotalAmount(), BigDecimal::add);
        }

        YearMonth thisMonth = YearMonth.now();
        YearMonth prevMonth = thisMonth.minusMonths(1);
        BigDecimal currentMonthTotal = approved.stream()
                .filter(e -> YearMonth.from(e.getExpenseDate()).equals(thisMonth))
                .map(Expense::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal previousMonthTotal = approved.stream()
                .filter(e -> YearMonth.from(e.getExpenseDate()).equals(prevMonth))
                .map(Expense::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaid = settlementRepository.findByRoomIdAndStatus(roomId, SettlementStatus.PAID).stream()
                .map(Settlement::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPending = settlementRepository.findByRoomIdAndStatus(roomId, SettlementStatus.PENDING).stream()
                .map(Settlement::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ExpenseAnalyticsResponse(
                toNamedAmounts(monthly), toNamedAmounts(category), toNamedAmounts(contribution),
                currentMonthTotal, previousMonthTotal, totalPaid, totalPending
        );
    }

    private List<NamedAmount> toNamedAmounts(Map<String, BigDecimal> map) {
        return map.entrySet().stream().map(e -> new NamedAmount(e.getKey(), e.getValue())).toList();
    }

    private BillResponse toBillResponse(Bill b) {
        return new BillResponse(b.getId(), b.getTitle(), b.getAmount(), b.getAmountPaid(), b.getDueDate(),
                b.getPaidBy() != null ? b.getPaidBy().getUser().getFullName() : null, b.getCategory().name(),
                b.getBillPhotoUrl(), b.getStatus().name());
    }

    private TaskResponse toTaskResponse(Task t) {
        return new TaskResponse(t.getId(), t.getTitle(), t.getDescription(), t.getAssignedTo().getId(),
                t.getAssignedTo().getUser().getFullName(), t.getDueDate(), t.getStatus().name(), t.getPriority().name(), t.getCompletedAt());
    }

    private CleaningScheduleResponse toCleaningResponse(CleaningSchedule s) {
        return new CleaningScheduleResponse(s.getId(), s.getTitle(), s.getCleaningDate(), s.getCleaningTime(),
                s.getAssignedTo().getId(), s.getAssignedTo().getUser().getFullName(), s.getTaskType().name(),
                s.getDescription(), s.getStatus().name(), s.getCompletedAt());
    }
}

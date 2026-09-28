package com.roommate.management.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MemberDashboardResponse(
        BigDecimal totalMonthlyExpenses,
        BigDecimal myContribution,
        BigDecimal myPendingAmount,
        List<BillResponse> upcomingBills,
        List<TaskResponse> myPendingTasks,
        LocalDate nextCleaningDate,
        long unreadMessages,
        long unreadNotifications,
        List<ExpenseResponse> recentExpenses,
        List<AnnouncementResponse> recentAnnouncements
) {}

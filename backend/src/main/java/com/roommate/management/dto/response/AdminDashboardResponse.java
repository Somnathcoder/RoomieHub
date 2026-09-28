package com.roommate.management.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record AdminDashboardResponse(
        long totalMembers,
        long activeMembers,
        BigDecimal monthlyExpenses,
        BigDecimal pendingPayments,
        long pendingExpenses,
        long pendingTasks,
        List<BillResponse> upcomingBills,
        List<CleaningScheduleResponse> upcomingCleaning,
        List<ActivityLogResponse> recentActivities,
        long openIssues
) {}

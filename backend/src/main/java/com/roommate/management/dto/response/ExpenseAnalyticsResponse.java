package com.roommate.management.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ExpenseAnalyticsResponse(
        List<NamedAmount> monthlyExpense,
        List<NamedAmount> categoryWise,
        List<NamedAmount> memberContribution,
        BigDecimal currentMonthTotal,
        BigDecimal previousMonthTotal,
        BigDecimal totalPaid,
        BigDecimal totalPending
) {}

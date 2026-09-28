package com.roommate.management.controller;

import com.roommate.management.dto.response.AdminDashboardResponse;
import com.roommate.management.dto.response.ExpenseAnalyticsResponse;
import com.roommate.management.dto.response.MemberDashboardResponse;
import com.roommate.management.exception.ApiResponse;
import com.roommate.management.security.SecurityUser;
import com.roommate.management.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/member")
    public ResponseEntity<ApiResponse<MemberDashboardResponse>> member(@AuthenticationPrincipal SecurityUser principal) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getMemberDashboard(principal.getId())));
    }

    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> admin(@AuthenticationPrincipal SecurityUser principal) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getAdminDashboard(principal.getId())));
    }

    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<ExpenseAnalyticsResponse>> analytics(@AuthenticationPrincipal SecurityUser principal) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getAnalytics(principal.getId())));
    }
}

package com.roommate.management.controller;

import com.roommate.management.dto.response.ActivityLogResponse;
import com.roommate.management.entity.enums.ActivityModule;
import com.roommate.management.exception.ApiResponse;
import com.roommate.management.security.SecurityUser;
import com.roommate.management.service.ActivityLogService;
import com.roommate.management.service.RoomAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/activity-logs")
@RequiredArgsConstructor
public class ActivityLogController {

    private final ActivityLogService activityLogService;
    private final RoomAccessService roomAccessService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ActivityLogResponse>>> list(
            @AuthenticationPrincipal SecurityUser principal,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) ActivityModule module,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        var member = roomAccessService.getActiveMembership(principal.getId());
        roomAccessService.requirePermission(member, com.roommate.management.entity.enums.PermissionCode.VIEW_ACTIVITY_LOG);
        return ResponseEntity.ok(ApiResponse.success(
                activityLogService.getRoomLogs(member.getRoom().getId(), userId, module, from, to)));
    }
}

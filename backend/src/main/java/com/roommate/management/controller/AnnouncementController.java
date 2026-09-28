package com.roommate.management.controller;

import com.roommate.management.dto.request.AnnouncementCreateRequest;
import com.roommate.management.dto.response.AnnouncementResponse;
import com.roommate.management.exception.ApiResponse;
import com.roommate.management.security.SecurityUser;
import com.roommate.management.service.AnnouncementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AnnouncementResponse>>> list(@AuthenticationPrincipal SecurityUser principal) {
        return ResponseEntity.ok(ApiResponse.success(announcementService.list(principal.getId())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AnnouncementResponse>> create(@AuthenticationPrincipal SecurityUser principal,
                                                                      @Valid @RequestBody AnnouncementCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Announcement posted", announcementService.create(principal.getId(), request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id) {
        announcementService.delete(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Announcement archived", null));
    }
}

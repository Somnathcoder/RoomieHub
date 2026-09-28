package com.roommate.management.controller;

import com.roommate.management.dto.request.*;
import com.roommate.management.dto.response.CleaningRotationResponse;
import com.roommate.management.dto.response.CleaningScheduleResponse;
import com.roommate.management.exception.ApiResponse;
import com.roommate.management.security.SecurityUser;
import com.roommate.management.service.CleaningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CleaningController {

    private final CleaningService cleaningService;

    @GetMapping("/cleaning-schedules")
    public ResponseEntity<ApiResponse<List<CleaningScheduleResponse>>> list(@AuthenticationPrincipal SecurityUser principal,
                                                                              @RequestParam(defaultValue = "false") boolean mine) {
        return ResponseEntity.ok(ApiResponse.success(cleaningService.list(principal.getId(), mine)));
    }

    @PostMapping("/cleaning-schedules")
    public ResponseEntity<ApiResponse<CleaningScheduleResponse>> create(@AuthenticationPrincipal SecurityUser principal,
                                                                          @Valid @RequestBody CleaningScheduleCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cleaning task scheduled", cleaningService.create(principal.getId(), request)));
    }

    @PutMapping("/cleaning-schedules/{id}")
    public ResponseEntity<ApiResponse<CleaningScheduleResponse>> update(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id,
                                                                          @RequestBody CleaningScheduleUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cleaning task updated", cleaningService.update(principal.getId(), id, request)));
    }

    @DeleteMapping("/cleaning-schedules/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id) {
        cleaningService.delete(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Cleaning task deleted", null));
    }

    @GetMapping("/cleaning-rotations")
    public ResponseEntity<ApiResponse<List<CleaningRotationResponse>>> listRotations(@AuthenticationPrincipal SecurityUser principal) {
        return ResponseEntity.ok(ApiResponse.success(cleaningService.listRotations(principal.getId())));
    }

    @PostMapping("/cleaning-rotations")
    public ResponseEntity<ApiResponse<CleaningRotationResponse>> createRotation(@AuthenticationPrincipal SecurityUser principal,
                                                                                  @Valid @RequestBody CleaningRotationCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Rotation created", cleaningService.createRotation(principal.getId(), request)));
    }

    @PutMapping("/cleaning-rotations/{id}")
    public ResponseEntity<ApiResponse<CleaningRotationResponse>> updateRotation(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id,
                                                                                  @RequestBody CleaningRotationUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Rotation updated", cleaningService.updateRotation(principal.getId(), id, request)));
    }
}

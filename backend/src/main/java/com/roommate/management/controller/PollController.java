package com.roommate.management.controller;

import com.roommate.management.dto.request.PollCreateRequest;
import com.roommate.management.dto.request.PollVoteRequest;
import com.roommate.management.dto.response.PollResponse;
import com.roommate.management.exception.ApiResponse;
import com.roommate.management.security.SecurityUser;
import com.roommate.management.service.PollService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/polls")
@RequiredArgsConstructor
public class PollController {

    private final PollService pollService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PollResponse>>> list(@AuthenticationPrincipal SecurityUser principal) {
        return ResponseEntity.ok(ApiResponse.success(pollService.list(principal.getId())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PollResponse>> create(@AuthenticationPrincipal SecurityUser principal,
                                                              @Valid @RequestBody PollCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Poll created", pollService.create(principal.getId(), request)));
    }

    @PostMapping("/{id}/vote")
    public ResponseEntity<ApiResponse<PollResponse>> vote(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id,
                                                            @Valid @RequestBody PollVoteRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Vote recorded", pollService.vote(principal.getId(), id, request)));
    }

    @PutMapping("/{id}/close")
    public ResponseEntity<ApiResponse<PollResponse>> close(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Poll closed", pollService.close(principal.getId(), id)));
    }
}

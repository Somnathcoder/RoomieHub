package com.roommate.management.controller;

import com.roommate.management.dto.request.IssueCreateRequest;
import com.roommate.management.dto.request.IssueUpdateStatusRequest;
import com.roommate.management.dto.response.FileUploadResponse;
import com.roommate.management.dto.response.IssueResponse;
import com.roommate.management.entity.enums.IssueStatus;
import com.roommate.management.exception.ApiResponse;
import com.roommate.management.security.SecurityUser;
import com.roommate.management.service.IssueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/issues")
@RequiredArgsConstructor
public class IssueController {

    private final IssueService issueService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<IssueResponse>>> list(@AuthenticationPrincipal SecurityUser principal,
                                                                   @RequestParam(required = false) IssueStatus status) {
        return ResponseEntity.ok(ApiResponse.success(issueService.list(principal.getId(), status)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<IssueResponse>> create(@AuthenticationPrincipal SecurityUser principal,
                                                               @Valid @RequestBody IssueCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Issue reported", issueService.create(principal.getId(), request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<IssueResponse>> updateStatus(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id,
                                                                     @Valid @RequestBody IssueUpdateStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Issue status updated", issueService.updateStatus(principal.getId(), id, request)));
    }

    @PostMapping(value = "/{id}/photo", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadPhoto(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id,
                                                                         @RequestParam("file") MultipartFile file) {
        String url = issueService.uploadPhoto(principal.getId(), id, file);
        return ResponseEntity.ok(ApiResponse.success("Photo uploaded", new FileUploadResponse(url, file.getOriginalFilename())));
    }
}

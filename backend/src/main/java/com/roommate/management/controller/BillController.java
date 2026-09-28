package com.roommate.management.controller;

import com.roommate.management.dto.request.BillCreateRequest;
import com.roommate.management.dto.request.BillUpdateRequest;
import com.roommate.management.dto.response.BillResponse;
import com.roommate.management.dto.response.FileUploadResponse;
import com.roommate.management.entity.enums.BillStatus;
import com.roommate.management.exception.ApiResponse;
import com.roommate.management.security.SecurityUser;
import com.roommate.management.service.BillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
public class BillController {

    private final BillService billService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BillResponse>>> list(@AuthenticationPrincipal SecurityUser principal,
                                                                  @RequestParam(required = false) BillStatus status) {
        return ResponseEntity.ok(ApiResponse.success(billService.list(principal.getId(), status)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BillResponse>> create(@AuthenticationPrincipal SecurityUser principal,
                                                              @Valid @RequestBody BillCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Bill created successfully", billService.create(principal.getId(), request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BillResponse>> update(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id,
                                                              @RequestBody BillUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Bill updated successfully", billService.update(principal.getId(), id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id) {
        billService.delete(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Bill deleted successfully", null));
    }

    @PostMapping(value = "/{id}/photo", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadPhoto(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id,
                                                                         @RequestParam("file") MultipartFile file) {
        String url = billService.uploadPhoto(principal.getId(), id, file);
        return ResponseEntity.ok(ApiResponse.success("Bill photo uploaded", new FileUploadResponse(url, file.getOriginalFilename())));
    }
}

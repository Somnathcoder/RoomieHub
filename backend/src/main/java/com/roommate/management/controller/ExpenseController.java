package com.roommate.management.controller;

import com.roommate.management.dto.request.*;
import com.roommate.management.dto.response.ExpenseResponse;
import com.roommate.management.dto.response.FileUploadResponse;
import com.roommate.management.entity.enums.ExpenseStatus;
import com.roommate.management.exception.ApiResponse;
import com.roommate.management.security.SecurityUser;
import com.roommate.management.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> list(@AuthenticationPrincipal SecurityUser principal,
                                                                     @RequestParam(required = false) ExpenseStatus status) {
        return ResponseEntity.ok(ApiResponse.success(expenseService.listExpenses(principal.getId(), status)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ExpenseResponse>> create(@AuthenticationPrincipal SecurityUser principal,
                                                                 @Valid @RequestBody ExpenseCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Expense created successfully", expenseService.createExpense(principal.getId(), request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpenseResponse>> get(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(expenseService.getExpense(principal.getId(), id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpenseResponse>> update(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id,
                                                                 @RequestBody ExpenseUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Expense updated successfully", expenseService.updateExpense(principal.getId(), id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id) {
        expenseService.deleteExpense(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Expense deleted successfully", null));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<ExpenseResponse>> decide(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id,
                                                                 @Valid @RequestBody ExpenseApprovalRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Expense decision recorded", expenseService.decideExpense(principal.getId(), id, request)));
    }

    @PostMapping(value = "/{id}/receipt", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadReceipt(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id,
                                                                           @RequestParam("file") MultipartFile file) {
        String url = expenseService.uploadReceipt(principal.getId(), id, file);
        return ResponseEntity.ok(ApiResponse.success("Receipt uploaded", new FileUploadResponse(url, file.getOriginalFilename())));
    }
}

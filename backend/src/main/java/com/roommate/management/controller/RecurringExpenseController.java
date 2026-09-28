package com.roommate.management.controller;

import com.roommate.management.dto.request.RecurringExpenseCreateRequest;
import com.roommate.management.dto.request.RecurringExpenseUpdateRequest;
import com.roommate.management.dto.response.RecurringExpenseResponse;
import com.roommate.management.exception.ApiResponse;
import com.roommate.management.security.SecurityUser;
import com.roommate.management.service.RecurringExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recurring-expenses")
@RequiredArgsConstructor
public class RecurringExpenseController {

    private final RecurringExpenseService recurringExpenseService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RecurringExpenseResponse>>> list(@AuthenticationPrincipal SecurityUser principal) {
        return ResponseEntity.ok(ApiResponse.success(recurringExpenseService.list(principal.getId())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RecurringExpenseResponse>> create(@AuthenticationPrincipal SecurityUser principal,
                                                                          @Valid @RequestBody RecurringExpenseCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Recurring expense created", recurringExpenseService.create(principal.getId(), request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RecurringExpenseResponse>> update(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id,
                                                                          @RequestBody RecurringExpenseUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Recurring expense updated", recurringExpenseService.update(principal.getId(), id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id) {
        recurringExpenseService.delete(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Recurring expense deleted", null));
    }
}

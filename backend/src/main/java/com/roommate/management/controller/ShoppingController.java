package com.roommate.management.controller;

import com.roommate.management.dto.request.ShoppingItemCreateRequest;
import com.roommate.management.dto.request.ShoppingItemUpdateRequest;
import com.roommate.management.dto.request.ShoppingToExpenseRequest;
import com.roommate.management.dto.response.ExpenseResponse;
import com.roommate.management.dto.response.FileUploadResponse;
import com.roommate.management.dto.response.ShoppingItemResponse;
import com.roommate.management.entity.enums.ShoppingStatus;
import com.roommate.management.exception.ApiResponse;
import com.roommate.management.security.SecurityUser;
import com.roommate.management.service.ShoppingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/shopping-items")
@RequiredArgsConstructor
public class ShoppingController {

    private final ShoppingService shoppingService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ShoppingItemResponse>>> list(@AuthenticationPrincipal SecurityUser principal,
                                                                          @RequestParam(required = false) ShoppingStatus status) {
        return ResponseEntity.ok(ApiResponse.success(shoppingService.list(principal.getId(), status)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ShoppingItemResponse>> add(@AuthenticationPrincipal SecurityUser principal,
                                                                   @Valid @RequestBody ShoppingItemCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Item added to shopping list", shoppingService.addItem(principal.getId(), request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ShoppingItemResponse>> update(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id,
                                                                      @RequestBody ShoppingItemUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Item updated", shoppingService.update(principal.getId(), id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id) {
        shoppingService.delete(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Item removed", null));
    }

    @PostMapping(value = "/{id}/item-photo", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadItemPhoto(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id,
                                                                             @RequestParam("file") MultipartFile file) {
        String url = shoppingService.uploadItemPhoto(principal.getId(), id, file);
        return ResponseEntity.ok(ApiResponse.success("Photo uploaded", new FileUploadResponse(url, file.getOriginalFilename())));
    }

    @PostMapping(value = "/{id}/bill-photo", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadBillPhoto(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id,
                                                                             @RequestParam("file") MultipartFile file) {
        String url = shoppingService.uploadBillPhoto(principal.getId(), id, file);
        return ResponseEntity.ok(ApiResponse.success("Bill photo uploaded", new FileUploadResponse(url, file.getOriginalFilename())));
    }

    @PostMapping("/{id}/convert-to-expense")
    public ResponseEntity<ApiResponse<ExpenseResponse>> convertToExpense(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id,
                                                                           @Valid @RequestBody ShoppingToExpenseRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Expense created from shopping item",
                shoppingService.convertToExpense(principal.getId(), id, request)));
    }
}

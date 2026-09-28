package com.roommate.management.controller;

import com.roommate.management.dto.request.InventoryItemCreateRequest;
import com.roommate.management.dto.request.InventoryItemUpdateRequest;
import com.roommate.management.dto.response.FileUploadResponse;
import com.roommate.management.dto.response.InventoryItemResponse;
import com.roommate.management.exception.ApiResponse;
import com.roommate.management.security.SecurityUser;
import com.roommate.management.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<InventoryItemResponse>>> list(@AuthenticationPrincipal SecurityUser principal) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.list(principal.getId())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InventoryItemResponse>> create(@AuthenticationPrincipal SecurityUser principal,
                                                                       @Valid @RequestBody InventoryItemCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Inventory item added", inventoryService.create(principal.getId(), request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> update(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id,
                                                                       @RequestBody InventoryItemUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Inventory item updated", inventoryService.update(principal.getId(), id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id) {
        inventoryService.delete(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Inventory item removed", null));
    }

    @PostMapping(value = "/{id}/photo", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadPhoto(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id,
                                                                         @RequestParam("file") MultipartFile file) {
        String url = inventoryService.uploadPhoto(principal.getId(), id, file);
        return ResponseEntity.ok(ApiResponse.success("Photo uploaded", new FileUploadResponse(url, file.getOriginalFilename())));
    }
}

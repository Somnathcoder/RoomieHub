package com.roommate.management.controller;

import com.roommate.management.dto.request.*;
import com.roommate.management.dto.response.FileUploadResponse;
import com.roommate.management.dto.response.MemberResponse;
import com.roommate.management.dto.response.PermissionResponse;
import com.roommate.management.exception.ApiResponse;
import com.roommate.management.security.SecurityUser;
import com.roommate.management.service.FileStorageService;
import com.roommate.management.service.MemberService;
import com.roommate.management.service.PermissionManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final PermissionManagementService permissionManagementService;
    private final FileStorageService fileStorageService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MemberResponse>>> list(@AuthenticationPrincipal SecurityUser principal) {
        return ResponseEntity.ok(ApiResponse.success(memberService.listMembers(principal.getId())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MemberResponse>> add(@AuthenticationPrincipal SecurityUser principal,
                                                             @Valid @RequestBody MemberCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Member added successfully", memberService.addMember(principal.getId(), request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MemberResponse>> get(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(memberService.getMember(principal.getId(), id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MemberResponse>> update(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id,
                                                                @RequestBody MemberUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Member updated successfully", memberService.updateMember(principal.getId(), id, request)));
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<ApiResponse<MemberResponse>> changeRole(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id,
                                                                    @Valid @RequestBody MemberRoleUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Role updated successfully", memberService.changeRole(principal.getId(), id, request)));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<MemberResponse>> updateStatus(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id,
                                                                      @Valid @RequestBody MemberStatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Status updated successfully", memberService.updateStatus(principal.getId(), id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<MemberResponse>> deactivate(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Member deactivated successfully",
                memberService.updateStatus(principal.getId(), id, new MemberStatusUpdateRequest(com.roommate.management.entity.enums.MemberStatus.DEACTIVATED))));
    }

    @PostMapping(value = "/{id}/photo", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadPhoto(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id,
                                                                         @RequestParam("file") MultipartFile file) {
        String url = memberService.uploadProfilePhoto(principal.getId(), id, file);
        return ResponseEntity.ok(ApiResponse.success("Profile photo uploaded", new FileUploadResponse(url, file.getOriginalFilename())));
    }

    @PostMapping(value = "/{id}/id-proof", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadIdProof(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id,
                                                                           @RequestParam("file") MultipartFile file) {
        String path = memberService.uploadIdProof(principal.getId(), id, file);
        return ResponseEntity.ok(ApiResponse.success("ID proof uploaded", new FileUploadResponse(path, file.getOriginalFilename())));
    }

    @GetMapping("/{id}/id-proof")
    public ResponseEntity<Resource> downloadIdProof(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id) {
        String relativePath = memberService.getIdProofPath(principal.getId(), id);
        try {
            Resource resource = new UrlResource(fileStorageService.resolve(relativePath).toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new com.roommate.management.exception.ResourceNotFoundException("ID proof file not found");
            }
            return ResponseEntity.ok()
                    .contentType(fileStorageService.detectContentType(relativePath))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .body(resource);
        } catch (MalformedURLException e) {
            throw new com.roommate.management.exception.ResourceNotFoundException("ID proof file not found");
        }
    }

    @GetMapping("/{id}/permissions")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getPermissions(@AuthenticationPrincipal SecurityUser principal,
                                                                                   @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(permissionManagementService.getMemberPermissions(principal.getId(), id)));
    }

    @PutMapping("/{id}/permissions")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> updatePermissions(@AuthenticationPrincipal SecurityUser principal,
                                                                                     @PathVariable Long id,
                                                                                     @Valid @RequestBody MemberPermissionsUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Permissions updated successfully",
                permissionManagementService.updateMemberPermissions(principal.getId(), id, request)));
    }
}

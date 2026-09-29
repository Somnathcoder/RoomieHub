package com.roommate.management.controller;

import com.roommate.management.dto.request.RoomCreateRequest;
import com.roommate.management.dto.response.RoomResponse;
import com.roommate.management.exception.ApiResponse;
import com.roommate.management.security.SecurityUser;
import com.roommate.management.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(@AuthenticationPrincipal SecurityUser principal,
                                                                  @Valid @RequestBody RoomCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Room created successfully", roomService.createRoom(principal.getId(), request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoom(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(roomService.getRoom(principal.getId(), id)));
    }
}

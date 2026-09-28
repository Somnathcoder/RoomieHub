package com.roommate.management.controller;

import com.roommate.management.dto.request.TaskCreateRequest;
import com.roommate.management.dto.request.TaskUpdateRequest;
import com.roommate.management.dto.response.TaskResponse;
import com.roommate.management.exception.ApiResponse;
import com.roommate.management.security.SecurityUser;
import com.roommate.management.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskResponse>>> list(@AuthenticationPrincipal SecurityUser principal,
                                                                  @RequestParam(defaultValue = "false") boolean mine) {
        return ResponseEntity.ok(ApiResponse.success(taskService.list(principal.getId(), mine)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponse>> create(@AuthenticationPrincipal SecurityUser principal,
                                                              @Valid @RequestBody TaskCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Task created successfully", taskService.create(principal.getId(), request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> update(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id,
                                                              @RequestBody TaskUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Task updated successfully", taskService.update(principal.getId(), id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id) {
        taskService.delete(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Task deleted successfully", null));
    }
}

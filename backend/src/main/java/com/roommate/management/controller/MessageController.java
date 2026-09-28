package com.roommate.management.controller;

import com.roommate.management.dto.request.PrivateMessageCreateRequest;
import com.roommate.management.dto.request.PublicMessageCreateRequest;
import com.roommate.management.dto.response.MessageResponse;
import com.roommate.management.exception.ApiResponse;
import com.roommate.management.security.SecurityUser;
import com.roommate.management.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getPublic(@AuthenticationPrincipal SecurityUser principal) {
        return ResponseEntity.ok(ApiResponse.success(messageService.getPublicMessages(principal.getId())));
    }

    @PostMapping("/public")
    public ResponseEntity<ApiResponse<MessageResponse>> sendPublic(@AuthenticationPrincipal SecurityUser principal,
                                                                     @Valid @RequestBody PublicMessageCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(messageService.sendPublic(principal.getId(), request)));
    }

    @GetMapping("/private/{userId}")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getConversation(@AuthenticationPrincipal SecurityUser principal,
                                                                                @PathVariable Long userId) {
        List<MessageResponse> conversation = messageService.getConversation(principal.getId(), userId);
        messageService.markConversationRead(principal.getId(), userId);
        return ResponseEntity.ok(ApiResponse.success(conversation));
    }

    @PostMapping("/private")
    public ResponseEntity<ApiResponse<MessageResponse>> sendPrivate(@AuthenticationPrincipal SecurityUser principal,
                                                                      @Valid @RequestBody PrivateMessageCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(messageService.sendPrivate(principal.getId(), request)));
    }
}

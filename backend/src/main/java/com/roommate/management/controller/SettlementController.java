package com.roommate.management.controller;

import com.roommate.management.dto.request.SettlementPayRequest;
import com.roommate.management.dto.response.MemberBalanceResponse;
import com.roommate.management.dto.response.SettlementResponse;
import com.roommate.management.dto.response.SettlementSummaryResponse;
import com.roommate.management.entity.enums.SettlementStatus;
import com.roommate.management.exception.ApiResponse;
import com.roommate.management.security.SecurityUser;
import com.roommate.management.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SettlementResponse>>> list(@AuthenticationPrincipal SecurityUser principal,
                                                                        @RequestParam(required = false) SettlementStatus status) {
        return ResponseEntity.ok(ApiResponse.success(settlementService.listSettlements(principal.getId(), status)));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<ApiResponse<SettlementResponse>> pay(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id,
                                                                 @RequestBody(required = false) SettlementPayRequest request) {
        SettlementPayRequest body = request == null ? new SettlementPayRequest(null) : request;
        return ResponseEntity.ok(ApiResponse.success("Marked as paid", settlementService.pay(principal.getId(), id, body)));
    }

    @PostMapping("/{id}/verify")
    public ResponseEntity<ApiResponse<SettlementResponse>> verify(@AuthenticationPrincipal SecurityUser principal, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Settlement verified", settlementService.verify(principal.getId(), id)));
    }

    @GetMapping("/balances")
    public ResponseEntity<ApiResponse<List<MemberBalanceResponse>>> balances(@AuthenticationPrincipal SecurityUser principal) {
        return ResponseEntity.ok(ApiResponse.success(settlementService.getBalances(principal.getId())));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<List<SettlementSummaryResponse>>> summary(@AuthenticationPrincipal SecurityUser principal) {
        return ResponseEntity.ok(ApiResponse.success(settlementService.getSummary(principal.getId())));
    }
}

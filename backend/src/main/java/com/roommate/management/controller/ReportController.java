package com.roommate.management.controller;

import com.roommate.management.security.SecurityUser;
import com.roommate.management.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/monthly-expense")
    public ResponseEntity<byte[]> monthlyExpense(@AuthenticationPrincipal SecurityUser principal,
                                                   @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate month,
                                                   @RequestParam(defaultValue = "csv") String format) {
        LocalDate target = month == null ? LocalDate.now() : month;
        String csv = reportService.monthlyExpenseCsv(principal.getId(), target);
        String fileBase = "monthly-expense-report-" + target.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return respond(csv, "Monthly Expense Report - " + target.getMonth() + " " + target.getYear(), fileBase, format);
    }

    @GetMapping("/member-contribution")
    public ResponseEntity<byte[]> memberContribution(@AuthenticationPrincipal SecurityUser principal,
                                                       @RequestParam(defaultValue = "csv") String format) {
        String csv = reportService.memberContributionCsv(principal.getId());
        return respond(csv, "Member Contribution Report", "member-contribution-report", format);
    }

    @GetMapping("/pending-settlement")
    public ResponseEntity<byte[]> pendingSettlement(@AuthenticationPrincipal SecurityUser principal,
                                                      @RequestParam(defaultValue = "csv") String format) {
        String csv = reportService.pendingSettlementCsv(principal.getId());
        return respond(csv, "Pending Settlement Report", "pending-settlement-report", format);
    }

    @GetMapping("/bill-report")
    public ResponseEntity<byte[]> billReport(@AuthenticationPrincipal SecurityUser principal,
                                               @RequestParam(defaultValue = "csv") String format) {
        String csv = reportService.billReportCsv(principal.getId());
        return respond(csv, "Bill Report", "bill-report", format);
    }

    private ResponseEntity<byte[]> respond(String csv, String title, String fileBase, String format) {
        if ("pdf".equalsIgnoreCase(format)) {
            byte[] pdf = reportService.toPdf(title, csv);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileBase + ".pdf").build().toString())
                    .body(pdf);
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileBase + ".csv").build().toString())
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }
}

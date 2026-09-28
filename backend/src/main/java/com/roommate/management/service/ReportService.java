package com.roommate.management.service;

import com.roommate.management.entity.*;
import com.roommate.management.entity.enums.ExpenseStatus;
import com.roommate.management.entity.enums.SettlementStatus;
import com.roommate.management.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final RoomAccessService roomAccessService;
    private final ExpenseRepository expenseRepository;
    private final SettlementRepository settlementRepository;
    private final BillRepository billRepository;

    @Transactional(readOnly = true)
    public String monthlyExpenseCsv(Long userId, LocalDate month) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        LocalDate start = month.withDayOfMonth(1);
        LocalDate end = month.withDayOfMonth(month.lengthOfMonth());
        List<Expense> expenses = expenseRepository.findByRoomIdAndExpenseDateBetween(caller.getRoom().getId(), start, end);

        StringBuilder csv = new StringBuilder("Date,Title,Category,Amount,Paid By,Status\n");
        for (Expense e : expenses) {
            csv.append(e.getExpenseDate()).append(',')
                    .append(escape(e.getTitle())).append(',')
                    .append(e.getCategory()).append(',')
                    .append(e.getTotalAmount()).append(',')
                    .append(escape(e.getPaidBy().getUser().getFullName())).append(',')
                    .append(e.getStatus()).append('\n');
        }
        return csv.toString();
    }

    @Transactional(readOnly = true)
    public String memberContributionCsv(Long userId) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        List<Expense> approved = expenseRepository.findByRoomIdOrderByExpenseDateDesc(caller.getRoom().getId()).stream()
                .filter(e -> e.getStatus() == ExpenseStatus.APPROVED).toList();

        java.util.Map<String, BigDecimal> byMember = new java.util.LinkedHashMap<>();
        for (Expense e : approved) {
            byMember.merge(e.getPaidBy().getUser().getFullName(), e.getTotalAmount(), BigDecimal::add);
        }
        StringBuilder csv = new StringBuilder("Member,Total Contribution\n");
        byMember.forEach((name, amount) -> csv.append(escape(name)).append(',').append(amount).append('\n'));
        return csv.toString();
    }

    @Transactional(readOnly = true)
    public String pendingSettlementCsv(Long userId) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        List<Settlement> pending = settlementRepository.findByRoomIdAndStatus(caller.getRoom().getId(), SettlementStatus.PENDING);

        StringBuilder csv = new StringBuilder("From,To,Amount,Related Expense\n");
        for (Settlement s : pending) {
            csv.append(escape(s.getFromMember().getUser().getFullName())).append(',')
                    .append(escape(s.getToMember().getUser().getFullName())).append(',')
                    .append(s.getAmount()).append(',')
                    .append(s.getRelatedExpense() != null ? escape(s.getRelatedExpense().getTitle()) : "").append('\n');
        }
        return csv.toString();
    }

    @Transactional(readOnly = true)
    public String billReportCsv(Long userId) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        List<Bill> bills = billRepository.findByRoomIdOrderByDueDateAsc(caller.getRoom().getId());

        StringBuilder csv = new StringBuilder("Title,Category,Amount,Amount Paid,Due Date,Status,Paid By\n");
        for (Bill b : bills) {
            csv.append(escape(b.getTitle())).append(',')
                    .append(b.getCategory()).append(',')
                    .append(b.getAmount()).append(',')
                    .append(b.getAmountPaid()).append(',')
                    .append(b.getDueDate()).append(',')
                    .append(b.getStatus()).append(',')
                    .append(b.getPaidBy() != null ? escape(b.getPaidBy().getUser().getFullName()) : "").append('\n');
        }
        return csv.toString();
    }

    /** Renders any of the above CSV reports as a simple, readable PDF (one line per row). */
    public byte[] toPdf(String title, String csv) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            String[] lines = csv.split("\n");
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDPageContentStream stream = new PDPageContentStream(document, page);
            float y = 780;
            stream.beginText();
            stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
            stream.newLineAtOffset(50, y);
            stream.showText(title);
            stream.endText();
            y -= 30;

            for (String line : lines) {
                if (y < 50) {
                    stream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    stream = new PDPageContentStream(document, page);
                    y = 780;
                }
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9);
                stream.newLineAtOffset(50, y);
                stream.showText(sanitizeForPdf(line));
                stream.endText();
                y -= 15;
            }
            stream.close();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String sanitizeForPdf(String s) {
        return s.replaceAll("[^\\x20-\\x7E]", "?");
    }

    private String escape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}

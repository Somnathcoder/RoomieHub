package com.roommate.management.scheduler;

import com.roommate.management.entity.Bill;
import com.roommate.management.entity.enums.BillStatus;
import com.roommate.management.entity.enums.NotificationType;
import com.roommate.management.repository.BillRepository;
import com.roommate.management.repository.RoomMemberRepository;
import com.roommate.management.entity.enums.MemberStatus;
import com.roommate.management.entity.RoomMember;
import com.roommate.management.service.BillService;
import com.roommate.management.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BillReminderScheduler {

    private final BillRepository billRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final NotificationService notificationService;
    private final BillService billService;

    @Scheduled(cron = "0 30 8 * * *") // every day at 08:30
    @Transactional
    public void remindAndMarkOverdue() {
        LocalDate today = LocalDate.now();
        LocalDate soon = today.plusDays(2);

        List<Bill> dueSoon = billRepository.findByStatusNotAndDueDateBefore(BillStatus.PAID, soon.plusDays(1));
        for (Bill bill : dueSoon) {
            if (bill.getDueDate().isBefore(today)) continue; // already overdue, handled separately below
            for (RoomMember member : roomMemberRepository.findByRoomIdAndStatus(bill.getRoom().getId(), MemberStatus.ACTIVE)) {
                notificationService.notify(member.getUser(), bill.getRoom(), NotificationType.BILL_DUE,
                        "Bill due soon", "\"" + bill.getTitle() + "\" of " + bill.getAmount() + " is due on " + bill.getDueDate() + ".",
                        bill.getId());
            }
        }

        billService.markOverdue(today);
    }
}

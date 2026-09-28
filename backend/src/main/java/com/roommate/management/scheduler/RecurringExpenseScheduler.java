package com.roommate.management.scheduler;

import com.roommate.management.service.RecurringExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class RecurringExpenseScheduler {

    private final RecurringExpenseService recurringExpenseService;

    @Scheduled(cron = "0 0 1 * * *") // every day at 01:00
    public void generateDueExpenses() {
        recurringExpenseService.generateDueExpenses(LocalDate.now());
    }
}

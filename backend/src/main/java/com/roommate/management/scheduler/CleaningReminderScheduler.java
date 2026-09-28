package com.roommate.management.scheduler;

import com.roommate.management.entity.CleaningSchedule;
import com.roommate.management.entity.enums.CleaningStatus;
import com.roommate.management.entity.enums.NotificationType;
import com.roommate.management.repository.CleaningScheduleRepository;
import com.roommate.management.service.CleaningService;
import com.roommate.management.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Sends cleaning reminders (day-before, on-day, overdue) and keeps active
 * rotations stocked with upcoming occurrences. Runs once a day.
 */
@Component
@RequiredArgsConstructor
public class CleaningReminderScheduler {

    private final CleaningScheduleRepository cleaningScheduleRepository;
    private final NotificationService notificationService;
    private final CleaningService cleaningService;

    @Scheduled(cron = "0 0 8 * * *") // every day at 08:00
    @Transactional
    public void sendReminders() {
        LocalDate today = LocalDate.now();

        List<CleaningSchedule> tomorrowTasks = cleaningScheduleRepository.findByCleaningDateAndReminderDayBeforeSentFalse(today.plusDays(1));
        for (CleaningSchedule s : tomorrowTasks) {
            notificationService.notify(s.getAssignedTo().getUser(), s.getRoom(), NotificationType.CLEANING_REMINDER,
                    "Cleaning task tomorrow",
                    "Reminder: your cleaning task \"" + s.getTitle() + "\" is scheduled tomorrow" +
                            (s.getCleaningTime() != null ? " at " + s.getCleaningTime() + "." : "."), s.getId());
            s.setReminderDayBeforeSent(true);
            cleaningScheduleRepository.save(s);
        }

        List<CleaningSchedule> todayTasks = cleaningScheduleRepository.findByCleaningDateAndReminderOnDaySentFalse(today);
        for (CleaningSchedule s : todayTasks) {
            notificationService.notify(s.getAssignedTo().getUser(), s.getRoom(), NotificationType.CLEANING_REMINDER,
                    "Today is your cleaning day", "Today is your cleaning day for \"" + s.getTitle() + "\".", s.getId());
            s.setReminderOnDaySent(true);
            cleaningScheduleRepository.save(s);
        }

        List<CleaningSchedule> overdueTasks = cleaningScheduleRepository
                .findByStatusNotAndCleaningDateBeforeAndReminderOverdueSentFalse(CleaningStatus.COMPLETED, today);
        for (CleaningSchedule s : overdueTasks) {
            notificationService.notify(s.getAssignedTo().getUser(), s.getRoom(), NotificationType.CLEANING_REMINDER,
                    "Cleaning task overdue", "Your cleaning task \"" + s.getTitle() + "\" is still pending.", s.getId());
            s.setReminderOverdueSent(true);
            cleaningScheduleRepository.save(s);
        }

        cleaningService.extendActiveRotations(today);
    }
}

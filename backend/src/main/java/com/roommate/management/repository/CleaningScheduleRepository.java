package com.roommate.management.repository;

import com.roommate.management.entity.CleaningSchedule;
import com.roommate.management.entity.enums.CleaningStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface CleaningScheduleRepository extends JpaRepository<CleaningSchedule, Long> {
    @EntityGraph(attributePaths = {"assignedTo.user"})
    List<CleaningSchedule> findByRoomIdOrderByCleaningDateAsc(Long roomId);
    List<CleaningSchedule> findByAssignedToIdOrderByCleaningDateAsc(Long roomMemberId);
    List<CleaningSchedule> findByCleaningDateAndReminderDayBeforeSentFalse(LocalDate date);
    List<CleaningSchedule> findByCleaningDateAndReminderOnDaySentFalse(LocalDate date);
    List<CleaningSchedule> findByStatusNotAndCleaningDateBeforeAndReminderOverdueSentFalse(CleaningStatus status, LocalDate date);
    List<CleaningSchedule> findByRoomIdAndCleaningDateBetween(Long roomId, LocalDate start, LocalDate end);
    List<CleaningSchedule> findByRotationIdOrderByCleaningDateAsc(Long rotationId);
    long countByRotationIdAndCleaningDateGreaterThanEqual(Long rotationId, LocalDate date);
}

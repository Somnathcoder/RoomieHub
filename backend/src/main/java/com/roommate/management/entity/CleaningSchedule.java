package com.roommate.management.entity;

import com.roommate.management.entity.enums.CleaningStatus;
import com.roommate.management.entity.enums.CleaningTaskType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cleaning_schedules")
public class CleaningSchedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "cleaning_date", nullable = false)
    private LocalDate cleaningDate;

    @Column(name = "cleaning_time")
    private LocalTime cleaningTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to", nullable = false)
    private RoomMember assignedTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 30)
    private CleaningTaskType taskType;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CleaningStatus status = CleaningStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rotation_id")
    private CleaningRotation rotation;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "reminder_day_before_sent", nullable = false)
    @Builder.Default
    private boolean reminderDayBeforeSent = false;

    @Column(name = "reminder_on_day_sent", nullable = false)
    @Builder.Default
    private boolean reminderOnDaySent = false;

    @Column(name = "reminder_overdue_sent", nullable = false)
    @Builder.Default
    private boolean reminderOverdueSent = false;
}

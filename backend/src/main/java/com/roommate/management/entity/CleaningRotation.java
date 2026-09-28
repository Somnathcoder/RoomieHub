package com.roommate.management.entity;

import com.roommate.management.entity.enums.CleaningTaskType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Configures an automatic weekly (or custom interval) cleaning rotation
 * across an ordered list of room members. memberOrderCsv stores an ordered,
 * comma separated list of room_member ids, e.g. "3,5,7,9".
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cleaning_rotations")
public class CleaningRotation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 30)
    private CleaningTaskType taskType;

    @Column(name = "member_order_csv", nullable = false, length = 500)
    private String memberOrderCsv;

    @Column(name = "current_index", nullable = false)
    @Builder.Default
    private int currentIndex = 0;

    @Column(name = "interval_days", nullable = false)
    @Builder.Default
    private int intervalDays = 7;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;
}

package com.roommate.management.entity;

import com.roommate.management.entity.enums.ActivityModule;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "activity_logs")
public class ActivityLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "action", nullable = false, length = 150)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(name = "module", nullable = false, length = 30)
    private ActivityModule module;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "description", length = 1000)
    private String description;
}

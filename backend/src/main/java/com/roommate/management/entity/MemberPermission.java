package com.roommate.management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Per-member permission override, used mainly to grant MODERATORs a
 * configurable subset of permissions. ADMIN members always have full access
 * regardless of rows here (enforced in service/security layer).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "member_permissions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_member_permission", columnNames = {"room_member_id", "permission_id"})
})
public class MemberPermission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_member_id", nullable = false)
    private RoomMember roomMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    @Column(name = "granted", nullable = false)
    @Builder.Default
    private boolean granted = true;
}

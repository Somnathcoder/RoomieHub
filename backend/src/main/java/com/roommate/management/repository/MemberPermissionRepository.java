package com.roommate.management.repository;

import com.roommate.management.entity.MemberPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberPermissionRepository extends JpaRepository<MemberPermission, Long> {
    List<MemberPermission> findByRoomMemberId(Long roomMemberId);
    Optional<MemberPermission> findByRoomMemberIdAndPermissionCode(Long roomMemberId, com.roommate.management.entity.enums.PermissionCode code);
    void deleteByRoomMemberId(Long roomMemberId);
}

package com.roommate.management.repository;

import com.roommate.management.entity.RoomMember;
import com.roommate.management.entity.enums.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {
    List<RoomMember> findByRoomId(Long roomId);
    List<RoomMember> findByRoomIdAndStatus(Long roomId, MemberStatus status);
    Optional<RoomMember> findByRoomIdAndUserId(Long roomId, Long userId);
    List<RoomMember> findByUserId(Long userId);
    Optional<RoomMember> findByUserIdAndStatus(Long userId, MemberStatus status);
    long countByRoomIdAndStatus(Long roomId, MemberStatus status);

    // Used by the admin dashboard's "total members" count - a COUNT instead of loading every
    // RoomMember row (each pulling in a lazy User/Room) just to call .size() on the list.
    long countByRoomId(Long roomId);
}

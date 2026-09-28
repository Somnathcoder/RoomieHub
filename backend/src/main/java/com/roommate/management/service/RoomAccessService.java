package com.roommate.management.service;

import com.roommate.management.entity.MemberPermission;
import com.roommate.management.entity.RoomMember;
import com.roommate.management.entity.enums.MemberStatus;
import com.roommate.management.entity.enums.PermissionCode;
import com.roommate.management.entity.enums.RoleType;
import com.roommate.management.exception.ForbiddenException;
import com.roommate.management.repository.MemberPermissionRepository;
import com.roommate.management.repository.RoomMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Central authorization gate. Every room-scoped operation must resolve the
 * caller's RoomMember through this service before touching any data, so a
 * user can never act on a room they don't belong to (room-level data
 * isolation) and MEMBER/MODERATOR users can never exceed their granted
 * permissions, no matter what the frontend does or doesn't hide.
 */
@Service
@RequiredArgsConstructor
public class RoomAccessService {

    private final RoomMemberRepository roomMemberRepository;
    private final MemberPermissionRepository memberPermissionRepository;

    /** Resolves the caller's single active room membership. MVP: one active room per user. */
    public RoomMember getActiveMembership(Long userId) {
        return roomMemberRepository.findByUserIdAndStatus(userId, MemberStatus.ACTIVE)
                .orElseThrow(() -> new ForbiddenException("You are not an active member of any room. Ask a room admin to add you."));
    }

    public RoomMember requireMembership(Long userId, Long roomId) {
        RoomMember member = roomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this room"));
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("Your membership in this room is not active");
        }
        return member;
    }

    public boolean hasPermission(RoomMember member, PermissionCode code) {
        if (member.getRole() == RoleType.ADMIN) {
            return true;
        }
        if (member.getRole() == RoleType.MODERATOR) {
            return memberPermissionRepository.findByRoomMemberIdAndPermissionCode(member.getId(), code)
                    .map(MemberPermission::isGranted)
                    .orElse(false);
        }
        return false;
    }

    public void requirePermission(RoomMember member, PermissionCode code) {
        if (!hasPermission(member, code)) {
            throw new ForbiddenException("You do not have permission to perform this action (" + code + ")");
        }
    }

    public void requireAdmin(RoomMember member) {
        if (member.getRole() != RoleType.ADMIN) {
            throw new ForbiddenException("Only the room admin can perform this action");
        }
    }

    public void requireSameRoom(RoomMember member, Long roomId) {
        if (!member.getRoom().getId().equals(roomId)) {
            throw new ForbiddenException("You cannot access data belonging to another room");
        }
    }
}

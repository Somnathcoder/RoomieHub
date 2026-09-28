package com.roommate.management.service;

import com.roommate.management.dto.request.MemberPermissionsUpdateRequest;
import com.roommate.management.dto.request.PermissionItem;
import com.roommate.management.dto.response.PermissionResponse;
import com.roommate.management.entity.MemberPermission;
import com.roommate.management.entity.Permission;
import com.roommate.management.entity.RoomMember;
import com.roommate.management.entity.enums.ActivityModule;
import com.roommate.management.entity.enums.PermissionCode;
import com.roommate.management.entity.enums.RoleType;
import com.roommate.management.exception.BadRequestException;
import com.roommate.management.exception.ResourceNotFoundException;
import com.roommate.management.repository.MemberPermissionRepository;
import com.roommate.management.repository.PermissionRepository;
import com.roommate.management.repository.RoomMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionManagementService {

    private final PermissionRepository permissionRepository;
    private final MemberPermissionRepository memberPermissionRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomAccessService roomAccessService;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public List<PermissionResponse> getMemberPermissions(Long callerUserId, Long roomMemberId) {
        RoomMember caller = roomAccessService.getActiveMembership(callerUserId);
        roomAccessService.requireAdmin(caller);

        RoomMember member = roomMemberRepository.findById(roomMemberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        roomAccessService.requireSameRoom(caller, member.getRoom().getId());

        List<MemberPermission> granted = memberPermissionRepository.findByRoomMemberId(roomMemberId);

        return permissionRepository.findAll().stream()
                .map(p -> {
                    boolean isGranted = member.getRole() == RoleType.ADMIN
                            || granted.stream().anyMatch(mp -> mp.getPermission().getId().equals(p.getId()) && mp.isGranted());
                    return new PermissionResponse(p.getCode().name(), p.getDescription(), isGranted);
                })
                .toList();
    }

    @Transactional
    public List<PermissionResponse> updateMemberPermissions(Long adminUserId, Long roomMemberId, MemberPermissionsUpdateRequest request) {
        RoomMember admin = roomAccessService.getActiveMembership(adminUserId);
        roomAccessService.requireAdmin(admin);

        RoomMember member = roomMemberRepository.findById(roomMemberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        roomAccessService.requireSameRoom(admin, member.getRoom().getId());

        if (member.getRole() == RoleType.ADMIN) {
            throw new BadRequestException("The room admin implicitly has all permissions and cannot be customized");
        }

        for (PermissionItem item : request.permissions()) {
            Permission permission = permissionRepository.findByCode(item.code())
                    .orElseThrow(() -> new ResourceNotFoundException("Unknown permission code: " + item.code()));

            MemberPermission mp = memberPermissionRepository
                    .findByRoomMemberIdAndPermissionCode(roomMemberId, item.code())
                    .orElseGet(() -> MemberPermission.builder().roomMember(member).permission(permission).build());
            mp.setGranted(item.granted());
            memberPermissionRepository.save(mp);
        }

        activityLogService.log(admin.getRoom(), admin.getUser(), ActivityModule.PERMISSION, "Permissions updated", roomMemberId,
                admin.getUser().getFullName() + " updated permissions for " + member.getUser().getFullName());

        return getMemberPermissions(adminUserId, roomMemberId);
    }

    public boolean hasPermission(RoomMember member, PermissionCode code) {
        return roomAccessService.hasPermission(member, code);
    }
}

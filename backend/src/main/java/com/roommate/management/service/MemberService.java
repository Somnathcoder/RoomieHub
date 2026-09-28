package com.roommate.management.service;

import com.roommate.management.dto.request.MemberCreateRequest;
import com.roommate.management.dto.request.MemberRoleUpdateRequest;
import com.roommate.management.dto.request.MemberStatusUpdateRequest;
import com.roommate.management.dto.request.MemberUpdateRequest;
import com.roommate.management.dto.response.MemberResponse;
import com.roommate.management.entity.RoomMember;
import com.roommate.management.entity.User;
import com.roommate.management.entity.enums.ActivityModule;
import com.roommate.management.entity.enums.MemberStatus;
import com.roommate.management.entity.enums.NotificationType;
import com.roommate.management.entity.enums.PermissionCode;
import com.roommate.management.entity.enums.RoleType;
import com.roommate.management.exception.BadRequestException;
import com.roommate.management.exception.ConflictException;
import com.roommate.management.exception.ForbiddenException;
import com.roommate.management.exception.ResourceNotFoundException;
import com.roommate.management.repository.RoomMemberRepository;
import com.roommate.management.repository.UserRepository;
import com.roommate.management.util.RandomPasswordGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoomAccessService roomAccessService;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;

    @Transactional
    public MemberResponse addMember(Long adminUserId, MemberCreateRequest request) {
        RoomMember admin = roomAccessService.getActiveMembership(adminUserId);
        roomAccessService.requirePermission(admin, PermissionCode.ADD_MEMBER);

        User user = userRepository.findByEmailIgnoreCase(request.email()).orElse(null);
        String rawPassword = request.password();
        boolean newUserCreated = user == null;
        if (user == null) {
            rawPassword = (rawPassword == null || rawPassword.isBlank()) ? RandomPasswordGenerator.generate(10) : rawPassword;
            user = User.builder()
                    .fullName(request.fullName())
                    .email(request.email().toLowerCase())
                    .password(passwordEncoder.encode(rawPassword))
                    .mobileNumber(request.mobileNumber())
                    .enabled(true)
                    .build();
            user = userRepository.save(user);
        } else if (roomMemberRepository.findByUserIdAndStatus(user.getId(), MemberStatus.ACTIVE).isPresent()) {
            throw new ConflictException("This person already belongs to an active room");
        }

        RoomMember member = RoomMember.builder()
                .room(admin.getRoom())
                .user(user)
                .role(request.role() == null ? RoleType.MEMBER : request.role())
                .roomNumber(request.roomNumber())
                .bedNumber(request.bedNumber())
                .joiningDate(request.joiningDate() == null ? LocalDate.now() : request.joiningDate())
                .status(MemberStatus.ACTIVE)
                .build();
        member = roomMemberRepository.save(member);

        activityLogService.log(admin.getRoom(), admin.getUser(), ActivityModule.MEMBER, "Member added", member.getId(),
                admin.getUser().getFullName() + " added " + user.getFullName() + " to the room");
        notificationService.notify(user, admin.getRoom(), NotificationType.MEMBER_ADDED,
                "Welcome to " + admin.getRoom().getRoomName(),
                "You have been added to the room by " + admin.getUser().getFullName() + ".", member.getId());

        // Surface the generated temporary password to the admin exactly once, in this response -
        // it's encoded before being persisted and can never be recovered afterwards.
        return toResponse(member, newUserCreated ? rawPassword : null);
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(Long userId) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        return roomMemberRepository.findByRoomId(caller.getRoom().getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public MemberResponse getMember(Long userId, Long roomMemberId) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        RoomMember target = roomMemberRepository.findById(roomMemberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        roomAccessService.requireSameRoom(caller, target.getRoom().getId());
        return toResponse(target);
    }

    @Transactional
    public MemberResponse updateMember(Long userId, Long roomMemberId, MemberUpdateRequest request) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        RoomMember target = roomMemberRepository.findById(roomMemberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        roomAccessService.requireSameRoom(caller, target.getRoom().getId());

        boolean isSelf = caller.getId().equals(target.getId());
        if (!isSelf && caller.getRole() != RoleType.ADMIN) {
            throw new ForbiddenException("Only the admin can edit another member's profile");
        }

        if (request.fullName() != null && !request.fullName().isBlank()) {
            target.getUser().setFullName(request.fullName());
            userRepository.save(target.getUser());
        }
        if (request.mobileNumber() != null) {
            target.getUser().setMobileNumber(request.mobileNumber());
            userRepository.save(target.getUser());
        }
        if (request.roomNumber() != null) target.setRoomNumber(request.roomNumber());
        if (request.bedNumber() != null) target.setBedNumber(request.bedNumber());

        target = roomMemberRepository.save(target);
        activityLogService.log(caller.getRoom(), caller.getUser(), ActivityModule.MEMBER, "Member updated", target.getId(),
                caller.getUser().getFullName() + " updated " + target.getUser().getFullName() + "'s profile");
        return toResponse(target);
    }

    @Transactional
    public MemberResponse changeRole(Long adminUserId, Long roomMemberId, MemberRoleUpdateRequest request) {
        RoomMember admin = roomAccessService.getActiveMembership(adminUserId);
        roomAccessService.requireAdmin(admin);

        RoomMember target = roomMemberRepository.findById(roomMemberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        roomAccessService.requireSameRoom(admin, target.getRoom().getId());

        if (target.getId().equals(admin.getId()) && request.role() != RoleType.ADMIN) {
            throw new BadRequestException("You cannot demote yourself. Transfer admin rights to another member first.");
        }
        target.setRole(request.role());
        target = roomMemberRepository.save(target);

        activityLogService.log(admin.getRoom(), admin.getUser(), ActivityModule.MEMBER, "Role changed", target.getId(),
                admin.getUser().getFullName() + " changed " + target.getUser().getFullName() + "'s role to " + request.role());
        return toResponse(target);
    }

    @Transactional
    public MemberResponse updateStatus(Long adminUserId, Long roomMemberId, MemberStatusUpdateRequest request) {
        RoomMember admin = roomAccessService.getActiveMembership(adminUserId);
        roomAccessService.requirePermission(admin, PermissionCode.REMOVE_MEMBER);

        RoomMember target = roomMemberRepository.findById(roomMemberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        roomAccessService.requireSameRoom(admin, target.getRoom().getId());

        if (target.getRole() == RoleType.ADMIN) {
            throw new BadRequestException("The room admin cannot be deactivated");
        }
        target.setStatus(request.status());
        target = roomMemberRepository.save(target);

        activityLogService.log(admin.getRoom(), admin.getUser(), ActivityModule.MEMBER, "Member status changed", target.getId(),
                admin.getUser().getFullName() + " set " + target.getUser().getFullName() + "'s status to " + request.status());
        return toResponse(target);
    }

    @Transactional
    public String uploadProfilePhoto(Long userId, Long roomMemberId, MultipartFile file) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        RoomMember target = roomMemberRepository.findById(roomMemberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        roomAccessService.requireSameRoom(caller, target.getRoom().getId());
        if (!caller.getId().equals(target.getId()) && caller.getRole() != RoleType.ADMIN) {
            throw new ForbiddenException("Only the member themselves or the admin can upload this photo");
        }
        String path = fileStorageService.store(file, "profile-photos",
                List.of("image/jpeg", "image/png", "image/jpg", "image/webp"));
        target.getUser().setProfilePhotoUrl(fileStorageService.toPublicUrl(path));
        userRepository.save(target.getUser());
        return target.getUser().getProfilePhotoUrl();
    }

    @Transactional
    public String uploadIdProof(Long userId, Long roomMemberId, MultipartFile file) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        RoomMember target = roomMemberRepository.findById(roomMemberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        roomAccessService.requireSameRoom(caller, target.getRoom().getId());
        if (!caller.getId().equals(target.getId()) && caller.getRole() != RoleType.ADMIN) {
            throw new ForbiddenException("Only the member themselves or the admin can upload ID proof");
        }
        String path = fileStorageService.store(file, "id-proofs",
                List.of("image/jpeg", "image/png", "image/jpg", "application/pdf"));
        // Stored as a relative path, not a public URL - access is gated through getIdProofPath() below.
        target.getUser().setIdProofUrl(path);
        userRepository.save(target.getUser());
        return path;
    }

    /** Only the member themselves or the room admin may view a member's sensitive ID proof document. */
    @Transactional(readOnly = true)
    public String getIdProofPath(Long userId, Long roomMemberId) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        RoomMember target = roomMemberRepository.findById(roomMemberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        roomAccessService.requireSameRoom(caller, target.getRoom().getId());
        if (!caller.getId().equals(target.getId()) && caller.getRole() != RoleType.ADMIN) {
            throw new ForbiddenException("You are not authorized to view this ID proof document");
        }
        if (target.getUser().getIdProofUrl() == null) {
            throw new ResourceNotFoundException("No ID proof has been uploaded for this member");
        }
        return target.getUser().getIdProofUrl();
    }

    private MemberResponse toResponse(RoomMember m) {
        return toResponse(m, null);
    }

    private MemberResponse toResponse(RoomMember m, String temporaryPassword) {
        User u = m.getUser();
        return new MemberResponse(
                m.getId(), u.getId(), u.getFullName(), u.getEmail(), u.getMobileNumber(),
                u.getProfilePhotoUrl(), u.getIdProofUrl() != null, m.getRoomNumber(), m.getBedNumber(),
                m.getJoiningDate(), m.getStatus().name(), m.getRole().name(), temporaryPassword
        );
    }
}

package com.roommate.management.service;

import com.roommate.management.dto.request.AnnouncementCreateRequest;
import com.roommate.management.dto.response.AnnouncementResponse;
import com.roommate.management.entity.Announcement;
import com.roommate.management.entity.RoomMember;
import com.roommate.management.entity.enums.*;
import com.roommate.management.exception.ResourceNotFoundException;
import com.roommate.management.repository.AnnouncementRepository;
import com.roommate.management.repository.RoomMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomAccessService roomAccessService;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;

    @Transactional
    public AnnouncementResponse create(Long userId, AnnouncementCreateRequest request) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        roomAccessService.requirePermission(caller, PermissionCode.MANAGE_ANNOUNCEMENT);

        Announcement announcement = Announcement.builder()
                .room(caller.getRoom())
                .title(request.title())
                .message(request.message())
                .imageUrl(request.imageUrl())
                .createdBy(caller)
                .status(AnnouncementStatus.ACTIVE)
                .build();
        announcement = announcementRepository.save(announcement);

        for (RoomMember member : roomMemberRepository.findByRoomIdAndStatus(caller.getRoom().getId(), MemberStatus.ACTIVE)) {
            if (!member.getId().equals(caller.getId())) {
                notificationService.notify(member.getUser(), caller.getRoom(), NotificationType.NEW_ANNOUNCEMENT,
                        announcement.getTitle(), announcement.getMessage(), announcement.getId());
            }
        }
        activityLogService.log(caller.getRoom(), caller.getUser(), ActivityModule.ANNOUNCEMENT, "Announcement created", announcement.getId(),
                caller.getUser().getFullName() + " posted announcement \"" + announcement.getTitle() + "\"");

        return toResponse(announcement);
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> list(Long userId) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        return announcementRepository.findByRoomIdOrderByCreatedAtDesc(caller.getRoom().getId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public void delete(Long userId, Long announcementId) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        roomAccessService.requirePermission(caller, PermissionCode.MANAGE_ANNOUNCEMENT);
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found"));
        roomAccessService.requireSameRoom(caller, announcement.getRoom().getId());
        announcement.setStatus(AnnouncementStatus.ARCHIVED);
        announcementRepository.save(announcement);
    }

    private AnnouncementResponse toResponse(Announcement a) {
        return new AnnouncementResponse(a.getId(), a.getTitle(), a.getMessage(), a.getCreatedBy().getUser().getFullName(),
                a.getImageUrl(), a.getStatus().name(), a.getCreatedAt());
    }
}

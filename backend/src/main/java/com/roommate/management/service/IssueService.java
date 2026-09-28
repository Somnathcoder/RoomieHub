package com.roommate.management.service;

import com.roommate.management.dto.request.IssueCreateRequest;
import com.roommate.management.dto.request.IssueUpdateStatusRequest;
import com.roommate.management.dto.response.IssueResponse;
import com.roommate.management.entity.Issue;
import com.roommate.management.entity.RoomMember;
import com.roommate.management.entity.enums.*;
import com.roommate.management.exception.ResourceNotFoundException;
import com.roommate.management.repository.IssueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IssueService {

    private final IssueRepository issueRepository;
    private final RoomAccessService roomAccessService;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;

    @Transactional
    public IssueResponse create(Long userId, IssueCreateRequest request) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        Issue issue = Issue.builder()
                .room(caller.getRoom())
                .title(request.title())
                .description(request.description())
                .priority(request.priority() == null ? IssuePriority.MEDIUM : request.priority())
                .reportedBy(caller)
                .status(IssueStatus.OPEN)
                .build();
        issue = issueRepository.save(issue);
        activityLogService.log(caller.getRoom(), caller.getUser(), ActivityModule.ISSUE, "Issue reported", issue.getId(),
                caller.getUser().getFullName() + " reported issue \"" + issue.getTitle() + "\"");
        return toResponse(issue);
    }

    @Transactional(readOnly = true)
    public List<IssueResponse> list(Long userId, IssueStatus status) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        List<Issue> issues = status == null
                ? issueRepository.findByRoomIdOrderByCreatedAtDesc(caller.getRoom().getId())
                : issueRepository.findByRoomIdAndStatus(caller.getRoom().getId(), status);
        return issues.stream().map(this::toResponse).toList();
    }

    @Transactional
    public IssueResponse updateStatus(Long userId, Long issueId, IssueUpdateStatusRequest request) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        roomAccessService.requirePermission(caller, PermissionCode.MANAGE_ISSUE);
        Issue issue = issueRepository.findById(issueId).orElseThrow(() -> new ResourceNotFoundException("Issue not found"));
        roomAccessService.requireSameRoom(caller, issue.getRoom().getId());

        issue.setStatus(request.status());
        if (request.status() == IssueStatus.RESOLVED) issue.setResolvedAt(LocalDateTime.now());
        issue = issueRepository.save(issue);

        notificationService.notify(issue.getReportedBy().getUser(), caller.getRoom(), NotificationType.ISSUE_STATUS_CHANGED,
                "Issue status updated", "\"" + issue.getTitle() + "\" is now " + issue.getStatus() + ".", issue.getId());
        activityLogService.log(caller.getRoom(), caller.getUser(), ActivityModule.ISSUE, "Issue status changed", issue.getId(),
                caller.getUser().getFullName() + " set issue \"" + issue.getTitle() + "\" to " + issue.getStatus());

        return toResponse(issue);
    }

    @Transactional
    public String uploadPhoto(Long userId, Long issueId, MultipartFile file) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        Issue issue = issueRepository.findById(issueId).orElseThrow(() -> new ResourceNotFoundException("Issue not found"));
        roomAccessService.requireSameRoom(caller, issue.getRoom().getId());
        String path = fileStorageService.store(file, "issues", List.of("image/jpeg", "image/png", "image/jpg"));
        issue.setPhotoUrl(fileStorageService.toPublicUrl(path));
        issueRepository.save(issue);
        return issue.getPhotoUrl();
    }

    private IssueResponse toResponse(Issue i) {
        return new IssueResponse(i.getId(), i.getTitle(), i.getDescription(), i.getPhotoUrl(), i.getPriority().name(),
                i.getReportedBy().getUser().getFullName(), i.getStatus().name(), i.getResolvedAt(), i.getCreatedAt());
    }
}

package com.roommate.management.service;

import com.roommate.management.dto.request.CleaningRotationCreateRequest;
import com.roommate.management.dto.request.CleaningRotationUpdateRequest;
import com.roommate.management.dto.request.CleaningScheduleCreateRequest;
import com.roommate.management.dto.request.CleaningScheduleUpdateRequest;
import com.roommate.management.dto.response.CleaningRotationResponse;
import com.roommate.management.dto.response.CleaningScheduleResponse;
import com.roommate.management.entity.CleaningRotation;
import com.roommate.management.entity.CleaningSchedule;
import com.roommate.management.entity.RoomMember;
import com.roommate.management.entity.enums.*;
import com.roommate.management.exception.BadRequestException;
import com.roommate.management.exception.ForbiddenException;
import com.roommate.management.exception.ResourceNotFoundException;
import com.roommate.management.repository.CleaningRotationRepository;
import com.roommate.management.repository.CleaningScheduleRepository;
import com.roommate.management.repository.RoomMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CleaningService {

    private static final int LOOKAHEAD_OCCURRENCES = 4;
    private static final int MIN_FUTURE_OCCURRENCES = 2;

    private final CleaningScheduleRepository cleaningScheduleRepository;
    private final CleaningRotationRepository cleaningRotationRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomAccessService roomAccessService;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;

    // ---------- Cleaning Schedule CRUD ----------

    @Transactional
    public CleaningScheduleResponse create(Long userId, CleaningScheduleCreateRequest request) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        roomAccessService.requirePermission(caller, PermissionCode.MANAGE_CLEANING);

        RoomMember assignee = roomMemberRepository.findById(request.assignedToMemberId())
                .orElseThrow(() -> new ResourceNotFoundException("Assignee not found"));
        roomAccessService.requireSameRoom(caller, assignee.getRoom().getId());

        CleaningSchedule schedule = CleaningSchedule.builder()
                .room(caller.getRoom())
                .title(request.title())
                .cleaningDate(request.cleaningDate())
                .cleaningTime(request.cleaningTime())
                .assignedTo(assignee)
                .taskType(request.taskType())
                .description(request.description())
                .status(CleaningStatus.PENDING)
                .build();
        schedule = cleaningScheduleRepository.save(schedule);

        notificationService.notify(assignee.getUser(), caller.getRoom(), NotificationType.CLEANING_REMINDER,
                "Cleaning task assigned", "\"" + schedule.getTitle() + "\" is scheduled on " + schedule.getCleaningDate() + ".", schedule.getId());
        activityLogService.log(caller.getRoom(), caller.getUser(), ActivityModule.CLEANING, "Cleaning scheduled", schedule.getId(),
                caller.getUser().getFullName() + " scheduled \"" + schedule.getTitle() + "\" for " + assignee.getUser().getFullName());

        return toResponse(schedule);
    }

    @Transactional(readOnly = true)
    public List<CleaningScheduleResponse> list(Long userId, boolean onlyMine) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        List<CleaningSchedule> schedules = onlyMine
                ? cleaningScheduleRepository.findByAssignedToIdOrderByCleaningDateAsc(caller.getId())
                : cleaningScheduleRepository.findByRoomIdOrderByCleaningDateAsc(caller.getRoom().getId());
        return schedules.stream().map(this::toResponse).toList();
    }

    @Transactional
    public CleaningScheduleResponse update(Long userId, Long scheduleId, CleaningScheduleUpdateRequest request) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        CleaningSchedule schedule = cleaningScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Cleaning schedule not found"));
        roomAccessService.requireSameRoom(caller, schedule.getRoom().getId());

        boolean isAssignee = schedule.getAssignedTo().getId().equals(caller.getId());
        boolean canManage = roomAccessService.hasPermission(caller, PermissionCode.MANAGE_CLEANING) || caller.getRole() == RoleType.ADMIN;
        if (!isAssignee && !canManage) {
            throw new ForbiddenException("You cannot update this cleaning task");
        }

        if (canManage) {
            if (request.title() != null) schedule.setTitle(request.title());
            if (request.cleaningDate() != null) schedule.setCleaningDate(request.cleaningDate());
            if (request.cleaningTime() != null) schedule.setCleaningTime(request.cleaningTime());
            if (request.taskType() != null) schedule.setTaskType(request.taskType());
            if (request.description() != null) schedule.setDescription(request.description());
            if (request.assignedToMemberId() != null) {
                RoomMember assignee = roomMemberRepository.findById(request.assignedToMemberId())
                        .orElseThrow(() -> new ResourceNotFoundException("Assignee not found"));
                roomAccessService.requireSameRoom(caller, assignee.getRoom().getId());
                schedule.setAssignedTo(assignee);
            }
        }
        if (request.status() != null) {
            schedule.setStatus(request.status());
            if (request.status() == CleaningStatus.COMPLETED) {
                schedule.setCompletedAt(LocalDateTime.now());
                activityLogService.log(caller.getRoom(), caller.getUser(), ActivityModule.CLEANING, "Cleaning completed", schedule.getId(),
                        caller.getUser().getFullName() + " marked \"" + schedule.getTitle() + "\" as completed");
            }
        }

        return toResponse(cleaningScheduleRepository.save(schedule));
    }

    @Transactional
    public void delete(Long userId, Long scheduleId) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        roomAccessService.requirePermission(caller, PermissionCode.MANAGE_CLEANING);
        CleaningSchedule schedule = cleaningScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Cleaning schedule not found"));
        roomAccessService.requireSameRoom(caller, schedule.getRoom().getId());
        cleaningScheduleRepository.delete(schedule);
    }

    // ---------- Rotation ----------

    @Transactional
    public CleaningRotationResponse createRotation(Long adminUserId, CleaningRotationCreateRequest request) {
        RoomMember admin = roomAccessService.getActiveMembership(adminUserId);
        roomAccessService.requirePermission(admin, PermissionCode.MANAGE_CLEANING);

        for (Long memberId : request.memberIds()) {
            RoomMember m = roomMemberRepository.findById(memberId)
                    .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + memberId));
            roomAccessService.requireSameRoom(admin, m.getRoom().getId());
        }

        CleaningRotation rotation = CleaningRotation.builder()
                .room(admin.getRoom())
                .taskType(request.taskType())
                .memberOrderCsv(request.memberIds().stream().map(String::valueOf).collect(Collectors.joining(",")))
                .intervalDays(request.intervalDays() <= 0 ? 7 : request.intervalDays())
                .startDate(request.startDate())
                .currentIndex(0)
                .active(true)
                .build();
        rotation = cleaningRotationRepository.save(rotation);

        generateOccurrences(rotation, LOOKAHEAD_OCCURRENCES);

        activityLogService.log(admin.getRoom(), admin.getUser(), ActivityModule.CLEANING, "Cleaning rotation created", rotation.getId(),
                admin.getUser().getFullName() + " set up a " + request.taskType() + " cleaning rotation");

        return toRotationResponse(rotation);
    }

    @Transactional(readOnly = true)
    public List<CleaningRotationResponse> listRotations(Long userId) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        return cleaningRotationRepository.findByRoomId(caller.getRoom().getId()).stream().map(this::toRotationResponse).toList();
    }

    @Transactional
    public CleaningRotationResponse updateRotation(Long adminUserId, Long rotationId, CleaningRotationUpdateRequest request) {
        RoomMember admin = roomAccessService.getActiveMembership(adminUserId);
        roomAccessService.requirePermission(admin, PermissionCode.MANAGE_CLEANING);
        CleaningRotation rotation = cleaningRotationRepository.findById(rotationId)
                .orElseThrow(() -> new ResourceNotFoundException("Rotation not found"));
        roomAccessService.requireSameRoom(admin, rotation.getRoom().getId());

        if (request.memberIds() != null && !request.memberIds().isEmpty()) {
            for (Long memberId : request.memberIds()) {
                RoomMember m = roomMemberRepository.findById(memberId)
                        .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + memberId));
                roomAccessService.requireSameRoom(admin, m.getRoom().getId());
            }
            rotation.setMemberOrderCsv(request.memberIds().stream().map(String::valueOf).collect(Collectors.joining(",")));
            rotation.setCurrentIndex(0);
        }
        if (request.intervalDays() != null && request.intervalDays() > 0) rotation.setIntervalDays(request.intervalDays());
        if (request.active() != null) rotation.setActive(request.active());

        rotation = cleaningRotationRepository.save(rotation);
        return toRotationResponse(rotation);
    }

    /** Called daily by the scheduler to keep every active rotation stocked with upcoming occurrences. */
    @Transactional
    public void extendActiveRotations(LocalDate today) {
        for (CleaningRotation rotation : cleaningRotationRepository.findByActiveTrue()) {
            long future = cleaningScheduleRepository.countByRotationIdAndCleaningDateGreaterThanEqual(rotation.getId(), today);
            if (future < MIN_FUTURE_OCCURRENCES) {
                generateOccurrences(rotation, 1);
            }
        }
    }

    private void generateOccurrences(CleaningRotation rotation, int count) {
        List<Long> order = parseOrder(rotation.getMemberOrderCsv());
        if (order.isEmpty()) {
            throw new BadRequestException("Rotation has no members configured");
        }
        List<CleaningSchedule> existing = cleaningScheduleRepository.findByRotationIdOrderByCleaningDateAsc(rotation.getId());
        LocalDate nextDate = existing.isEmpty()
                ? rotation.getStartDate()
                : existing.get(existing.size() - 1).getCleaningDate().plusDays(rotation.getIntervalDays());
        int idx = rotation.getCurrentIndex();

        for (int i = 0; i < count; i++) {
            Long memberId = order.get(idx % order.size());
            RoomMember assignee = roomMemberRepository.findById(memberId).orElseThrow();
            CleaningSchedule schedule = CleaningSchedule.builder()
                    .room(rotation.getRoom())
                    .title(formatTaskType(rotation.getTaskType()) + " Cleaning")
                    .cleaningDate(nextDate)
                    .assignedTo(assignee)
                    .taskType(rotation.getTaskType())
                    .description("Auto-generated from cleaning rotation")
                    .status(CleaningStatus.PENDING)
                    .rotation(rotation)
                    .build();
            cleaningScheduleRepository.save(schedule);

            notificationService.notify(assignee.getUser(), rotation.getRoom(), NotificationType.CLEANING_REMINDER,
                    "Upcoming cleaning duty", "You're on rotation for " + formatTaskType(rotation.getTaskType()) + " cleaning on " + nextDate + ".",
                    schedule.getId());

            idx++;
            nextDate = nextDate.plusDays(rotation.getIntervalDays());
        }
        rotation.setCurrentIndex(idx % order.size());
        cleaningRotationRepository.save(rotation);
    }

    private List<Long> parseOrder(String csv) {
        return Arrays.stream(csv.split(",")).filter(s -> !s.isBlank()).map(Long::parseLong).toList();
    }

    private String formatTaskType(CleaningTaskType type) {
        return type.name().replace('_', ' ');
    }

    private CleaningScheduleResponse toResponse(CleaningSchedule s) {
        return new CleaningScheduleResponse(
                s.getId(), s.getTitle(), s.getCleaningDate(), s.getCleaningTime(), s.getAssignedTo().getId(),
                s.getAssignedTo().getUser().getFullName(), s.getTaskType().name(), s.getDescription(),
                s.getStatus().name(), s.getCompletedAt()
        );
    }

    private CleaningRotationResponse toRotationResponse(CleaningRotation r) {
        List<Long> ids = parseOrder(r.getMemberOrderCsv());
        List<String> names = ids.stream()
                .map(id -> roomMemberRepository.findById(id).map(m -> m.getUser().getFullName()).orElse("Unknown"))
                .toList();
        return new CleaningRotationResponse(r.getId(), r.getTaskType().name(), names, ids, r.getIntervalDays(),
                r.getStartDate(), r.isActive(), r.getCurrentIndex());
    }
}

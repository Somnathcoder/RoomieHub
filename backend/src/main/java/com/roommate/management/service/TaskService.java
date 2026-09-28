package com.roommate.management.service;

import com.roommate.management.dto.request.TaskCreateRequest;
import com.roommate.management.dto.request.TaskUpdateRequest;
import com.roommate.management.dto.response.TaskResponse;
import com.roommate.management.entity.RoomMember;
import com.roommate.management.entity.Task;
import com.roommate.management.entity.enums.*;
import com.roommate.management.exception.ForbiddenException;
import com.roommate.management.exception.ResourceNotFoundException;
import com.roommate.management.repository.RoomMemberRepository;
import com.roommate.management.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomAccessService roomAccessService;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;

    @Transactional
    public TaskResponse create(Long userId, TaskCreateRequest request) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        roomAccessService.requirePermission(caller, PermissionCode.MANAGE_TASK);

        RoomMember assignee = roomMemberRepository.findById(request.assignedToMemberId())
                .orElseThrow(() -> new ResourceNotFoundException("Assignee not found"));
        roomAccessService.requireSameRoom(caller, assignee.getRoom().getId());

        Task task = Task.builder()
                .room(caller.getRoom())
                .title(request.title())
                .description(request.description())
                .assignedTo(assignee)
                .dueDate(request.dueDate())
                .priority(request.priority() == null ? TaskPriority.MEDIUM : request.priority())
                .status(TaskStatus.PENDING)
                .createdBy(caller)
                .build();
        task = taskRepository.save(task);

        notificationService.notify(assignee.getUser(), caller.getRoom(), NotificationType.TASK_ASSIGNED,
                "New task assigned", "\"" + task.getTitle() + "\" was assigned to you.", task.getId());
        activityLogService.log(caller.getRoom(), caller.getUser(), ActivityModule.TASK, "Task created", task.getId(),
                caller.getUser().getFullName() + " assigned \"" + task.getTitle() + "\" to " + assignee.getUser().getFullName());

        return toResponse(task);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list(Long userId, boolean onlyMine) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        List<Task> tasks = onlyMine
                ? taskRepository.findByAssignedToIdOrderByDueDateAsc(caller.getId())
                : taskRepository.findByRoomIdOrderByDueDateAsc(caller.getRoom().getId());
        return tasks.stream().map(this::toResponse).toList();
    }

    @Transactional
    public TaskResponse update(Long userId, Long taskId, TaskUpdateRequest request) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        roomAccessService.requireSameRoom(caller, task.getRoom().getId());

        boolean isAssignee = task.getAssignedTo().getId().equals(caller.getId());
        boolean canManage = roomAccessService.hasPermission(caller, PermissionCode.MANAGE_TASK) || caller.getRole() == RoleType.ADMIN;
        if (!isAssignee && !canManage) {
            throw new ForbiddenException("You cannot update this task");
        }

        if (canManage) {
            if (request.title() != null) task.setTitle(request.title());
            if (request.description() != null) task.setDescription(request.description());
            if (request.dueDate() != null) task.setDueDate(request.dueDate());
            if (request.priority() != null) task.setPriority(request.priority());
            if (request.assignedToMemberId() != null) {
                RoomMember assignee = roomMemberRepository.findById(request.assignedToMemberId())
                        .orElseThrow(() -> new ResourceNotFoundException("Assignee not found"));
                roomAccessService.requireSameRoom(caller, assignee.getRoom().getId());
                task.setAssignedTo(assignee);
            }
        }
        if (request.status() != null) {
            task.setStatus(request.status());
            if (request.status() == TaskStatus.COMPLETED) task.setCompletedAt(LocalDateTime.now());
        }

        task = taskRepository.save(task);
        activityLogService.log(caller.getRoom(), caller.getUser(), ActivityModule.TASK, "Task updated", task.getId(),
                caller.getUser().getFullName() + " updated task \"" + task.getTitle() + "\"");
        return toResponse(task);
    }

    @Transactional
    public void delete(Long userId, Long taskId) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        roomAccessService.requirePermission(caller, PermissionCode.MANAGE_TASK);
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        roomAccessService.requireSameRoom(caller, task.getRoom().getId());
        taskRepository.delete(task);
    }

    private TaskResponse toResponse(Task t) {
        return new TaskResponse(
                t.getId(), t.getTitle(), t.getDescription(), t.getAssignedTo().getId(),
                t.getAssignedTo().getUser().getFullName(), t.getDueDate(), t.getStatus().name(),
                t.getPriority().name(), t.getCompletedAt()
        );
    }
}

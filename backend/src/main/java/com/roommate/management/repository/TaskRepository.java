package com.roommate.management.repository;

import com.roommate.management.entity.Task;
import com.roommate.management.entity.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByRoomIdOrderByDueDateAsc(Long roomId);
    List<Task> findByAssignedToIdOrderByDueDateAsc(Long roomMemberId);
    List<Task> findByRoomIdAndStatus(Long roomId, TaskStatus status);
}

package com.roommate.management.repository;

import com.roommate.management.entity.Task;
import com.roommate.management.entity.enums.TaskStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    @EntityGraph(attributePaths = {"assignedTo.user"})
    List<Task> findByRoomIdOrderByDueDateAsc(Long roomId);
    List<Task> findByAssignedToIdOrderByDueDateAsc(Long roomMemberId);
    List<Task> findByRoomIdAndStatus(Long roomId, TaskStatus status);

    // Used by the admin dashboard's "pending tasks" count - a COUNT at the DB instead of
    // loading every task row in the room just to filter and count them in Java.
    long countByRoomIdAndStatusNot(Long roomId, TaskStatus status);
}

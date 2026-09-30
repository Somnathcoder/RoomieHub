package com.roommate.management.repository;

import com.roommate.management.entity.Issue;
import com.roommate.management.entity.enums.IssueStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueRepository extends JpaRepository<Issue, Long> {
    @EntityGraph(attributePaths = {"reportedBy.user"})
    List<Issue> findByRoomIdOrderByCreatedAtDesc(Long roomId);
    List<Issue> findByRoomIdAndStatus(Long roomId, IssueStatus status);

    // Used by the admin dashboard's "open issues" count - see TaskRepository.countByRoomIdAndStatusNot.
    long countByRoomIdAndStatusNot(Long roomId, IssueStatus status);
}

package com.roommate.management.repository;

import com.roommate.management.entity.Issue;
import com.roommate.management.entity.enums.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueRepository extends JpaRepository<Issue, Long> {
    List<Issue> findByRoomIdOrderByCreatedAtDesc(Long roomId);
    List<Issue> findByRoomIdAndStatus(Long roomId, IssueStatus status);
}

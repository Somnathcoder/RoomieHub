package com.roommate.management.repository;

import com.roommate.management.entity.Poll;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PollRepository extends JpaRepository<Poll, Long> {
    @EntityGraph(attributePaths = {"createdBy.user"})
    List<Poll> findByRoomIdOrderByCreatedAtDesc(Long roomId);
}

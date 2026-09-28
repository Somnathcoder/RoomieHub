package com.roommate.management.repository;

import com.roommate.management.entity.Poll;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PollRepository extends JpaRepository<Poll, Long> {
    List<Poll> findByRoomIdOrderByCreatedAtDesc(Long roomId);
}

package com.roommate.management.repository;

import com.roommate.management.entity.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PollVoteRepository extends JpaRepository<PollVote, Long> {
    List<PollVote> findByPollId(Long pollId);
    Optional<PollVote> findByPollIdAndVoterId(Long pollId, Long voterId);
    long countByOptionId(Long optionId);
}

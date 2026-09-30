package com.roommate.management.repository;

import com.roommate.management.entity.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PollVoteRepository extends JpaRepository<PollVote, Long> {
    List<PollVote> findByPollId(Long pollId);
    Optional<PollVote> findByPollIdAndVoterId(Long pollId, Long voterId);
    long countByOptionId(Long optionId);

    // Batch variants for listing N polls at once (see PollService.list()) - each replaces what
    // used to be one query per option (vote counts) or one query per poll (the caller's own
    // vote) with a single grouped/filtered query covering the whole list.
    @Query("SELECT v.option.id, COUNT(v) FROM PollVote v WHERE v.option.id IN :optionIds GROUP BY v.option.id")
    List<Object[]> countByOptionIds(@Param("optionIds") List<Long> optionIds);

    List<PollVote> findByPollIdInAndVoterId(List<Long> pollIds, Long voterId);
}

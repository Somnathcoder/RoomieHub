package com.roommate.management.repository;

import com.roommate.management.entity.PollOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PollOptionRepository extends JpaRepository<PollOption, Long> {
    List<PollOption> findByPollId(Long pollId);

    // Batch variant for listing N polls at once - one query for every option across all of
    // them instead of one findByPollId call per poll.
    List<PollOption> findByPollIdIn(List<Long> pollIds);
}

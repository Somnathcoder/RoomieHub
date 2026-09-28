package com.roommate.management.service;

import com.roommate.management.dto.request.PollCreateRequest;
import com.roommate.management.dto.request.PollVoteRequest;
import com.roommate.management.dto.response.PollOptionResponse;
import com.roommate.management.dto.response.PollResponse;
import com.roommate.management.entity.Poll;
import com.roommate.management.entity.PollOption;
import com.roommate.management.entity.PollVote;
import com.roommate.management.entity.RoomMember;
import com.roommate.management.entity.enums.*;
import com.roommate.management.exception.BadRequestException;
import com.roommate.management.exception.ForbiddenException;
import com.roommate.management.exception.ResourceNotFoundException;
import com.roommate.management.repository.PollOptionRepository;
import com.roommate.management.repository.PollRepository;
import com.roommate.management.repository.PollVoteRepository;
import com.roommate.management.repository.RoomMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PollService {

    private final PollRepository pollRepository;
    private final PollOptionRepository pollOptionRepository;
    private final PollVoteRepository pollVoteRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomAccessService roomAccessService;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;

    @Transactional
    public PollResponse create(Long userId, PollCreateRequest request) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        roomAccessService.requirePermission(caller, PermissionCode.MANAGE_POLL);

        if (request.options().size() < 2) {
            throw new BadRequestException("A poll needs at least two options");
        }

        Poll poll = Poll.builder()
                .room(caller.getRoom())
                .question(request.question())
                .createdBy(caller)
                .status(PollStatus.OPEN)
                .closesAt(request.closesAt())
                .build();
        poll = pollRepository.save(poll);

        for (String optionText : request.options()) {
            pollOptionRepository.save(PollOption.builder().poll(poll).optionText(optionText).build());
        }

        for (RoomMember member : roomMemberRepository.findByRoomIdAndStatus(caller.getRoom().getId(), MemberStatus.ACTIVE)) {
            if (!member.getId().equals(caller.getId())) {
                notificationService.notify(member.getUser(), caller.getRoom(), NotificationType.POLL_CREATED,
                        "New poll: " + poll.getQuestion(), "Vote now.", poll.getId());
            }
        }
        activityLogService.log(caller.getRoom(), caller.getUser(), ActivityModule.POLL, "Poll created", poll.getId(),
                caller.getUser().getFullName() + " created poll \"" + poll.getQuestion() + "\"");

        return toResponse(poll, userId);
    }

    @Transactional(readOnly = true)
    public List<PollResponse> list(Long userId) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        return pollRepository.findByRoomIdOrderByCreatedAtDesc(caller.getRoom().getId())
                .stream().map(p -> toResponse(p, userId)).toList();
    }

    @Transactional
    public PollResponse vote(Long userId, Long pollId, PollVoteRequest request) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        Poll poll = pollRepository.findById(pollId).orElseThrow(() -> new ResourceNotFoundException("Poll not found"));
        roomAccessService.requireSameRoom(caller, poll.getRoom().getId());

        if (poll.getStatus() != PollStatus.OPEN) {
            throw new BadRequestException("This poll is closed");
        }
        if (pollVoteRepository.findByPollIdAndVoterId(pollId, caller.getId()).isPresent()) {
            throw new BadRequestException("You have already voted in this poll");
        }
        PollOption option = pollOptionRepository.findById(request.optionId())
                .filter(o -> o.getPoll().getId().equals(pollId))
                .orElseThrow(() -> new ResourceNotFoundException("Option not found in this poll"));

        pollVoteRepository.save(PollVote.builder().poll(poll).option(option).voter(caller).build());
        return toResponse(poll, userId);
    }

    @Transactional
    public PollResponse close(Long userId, Long pollId) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        roomAccessService.requirePermission(caller, PermissionCode.MANAGE_POLL);
        Poll poll = pollRepository.findById(pollId).orElseThrow(() -> new ResourceNotFoundException("Poll not found"));
        roomAccessService.requireSameRoom(caller, poll.getRoom().getId());
        poll.setStatus(PollStatus.CLOSED);
        poll = pollRepository.save(poll);
        return toResponse(poll, userId);
    }

    private PollResponse toResponse(Poll poll, Long userId) {
        List<PollOption> options = pollOptionRepository.findByPollId(poll.getId());
        List<PollOptionResponse> optionResponses = options.stream()
                .map(o -> new PollOptionResponse(o.getId(), o.getOptionText(), pollVoteRepository.countByOptionId(o.getId())))
                .toList();
        long totalVotes = optionResponses.stream().mapToLong(PollOptionResponse::voteCount).sum();
        Optional<PollVote> myVote = pollVoteRepository.findByPollIdAndVoterId(poll.getId(),
                roomMemberRepository.findByUserIdAndStatus(userId, MemberStatus.ACTIVE).map(RoomMember::getId).orElse(-1L));
        return new PollResponse(poll.getId(), poll.getQuestion(), poll.getCreatedBy().getUser().getFullName(),
                poll.getStatus().name(), poll.getClosesAt(), optionResponses, totalVotes,
                myVote.map(v -> v.getOption().getId()).orElse(null));
    }
}

package com.roommate.management.service;

import com.roommate.management.dto.request.RoomCreateRequest;
import com.roommate.management.dto.response.RoomResponse;
import com.roommate.management.entity.Room;
import com.roommate.management.entity.RoomMember;
import com.roommate.management.entity.User;
import com.roommate.management.entity.enums.ActivityModule;
import com.roommate.management.entity.enums.MemberStatus;
import com.roommate.management.entity.enums.RoleType;
import com.roommate.management.entity.enums.RoomStatus;
import com.roommate.management.exception.ConflictException;
import com.roommate.management.exception.ResourceNotFoundException;
import com.roommate.management.repository.RoomMemberRepository;
import com.roommate.management.repository.RoomRepository;
import com.roommate.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final RoomAccessService roomAccessService;

    @Transactional
    public RoomResponse createRoom(Long userId, RoomCreateRequest request) {
        if (roomMemberRepository.findByUserIdAndStatus(userId, MemberStatus.ACTIVE).isPresent()) {
            throw new ConflictException("You already belong to an active room. Leave or be removed from it first.");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Room room = Room.builder()
                .roomName(request.roomName())
                .address(request.address())
                .createdBy(user)
                .status(RoomStatus.ACTIVE)
                .build();
        room = roomRepository.save(room);

        RoomMember admin = RoomMember.builder()
                .room(room)
                .user(user)
                .role(RoleType.ADMIN)
                .joiningDate(LocalDate.now())
                .status(MemberStatus.ACTIVE)
                .build();
        roomMemberRepository.save(admin);

        activityLogService.log(room, user, ActivityModule.ROOM, "Room created", room.getId(),
                user.getFullName() + " created room \"" + room.getRoomName() + "\"");

        return toResponse(room);
    }

    @Transactional(readOnly = true)
    public RoomResponse getRoom(Long userId, Long roomId) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        roomAccessService.requireSameRoom(caller, roomId);
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new ResourceNotFoundException("Room not found"));
        return toResponse(room);
    }

    private RoomResponse toResponse(Room room) {
        long memberCount = roomMemberRepository.countByRoomIdAndStatus(room.getId(), MemberStatus.ACTIVE);
        return new RoomResponse(
                room.getId(), room.getRoomName(), room.getAddress(),
                room.getCreatedBy().getFullName(), room.getCreatedAt(), room.getStatus().name(), memberCount
        );
    }
}

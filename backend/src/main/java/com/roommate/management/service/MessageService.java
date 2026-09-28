package com.roommate.management.service;

import com.roommate.management.dto.request.PrivateMessageCreateRequest;
import com.roommate.management.dto.request.PublicMessageCreateRequest;
import com.roommate.management.dto.response.MessageResponse;
import com.roommate.management.entity.Message;
import com.roommate.management.entity.RoomMember;
import com.roommate.management.entity.User;
import com.roommate.management.entity.enums.MessageType;
import com.roommate.management.entity.enums.NotificationType;
import com.roommate.management.entity.enums.PermissionCode;
import com.roommate.management.exception.ForbiddenException;
import com.roommate.management.exception.ResourceNotFoundException;
import com.roommate.management.repository.MessageRepository;
import com.roommate.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final RoomAccessService roomAccessService;
    private final NotificationService notificationService;

    @Transactional
    public MessageResponse sendPublic(Long userId, PublicMessageCreateRequest request) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        Message message = Message.builder()
                .room(caller.getRoom())
                .sender(caller.getUser())
                .type(MessageType.PUBLIC)
                .content(request.content())
                .isRead(true)
                .build();
        return toResponse(messageRepository.save(message));
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getPublicMessages(Long userId) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        return messageRepository.findByRoomIdAndTypeOrderByCreatedAtAsc(caller.getRoom().getId(), MessageType.PUBLIC)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public MessageResponse sendPrivate(Long userId, PrivateMessageCreateRequest request) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        User receiver = userRepository.findById(request.receiverUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Recipient not found"));
        // Ensure the recipient is in the same room to keep private messaging inside one household.
        roomAccessService.requireMembership(receiver.getId(), caller.getRoom().getId());

        Message message = Message.builder()
                .room(caller.getRoom())
                .sender(caller.getUser())
                .receiver(receiver)
                .type(MessageType.PRIVATE)
                .content(request.content())
                .isRead(false)
                .build();
        message = messageRepository.save(message);

        notificationService.notify(receiver, caller.getRoom(), NotificationType.NEW_PRIVATE_MESSAGE,
                "New message from " + caller.getUser().getFullName(), request.content(), message.getId());

        return toResponse(message);
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getConversation(Long userId, Long otherUserId) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        roomAccessService.requireMembership(otherUserId, caller.getRoom().getId());
        return messageRepository.findConversation(userId, otherUserId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public void markConversationRead(Long userId, Long otherUserId) {
        List<Message> conversation = messageRepository.findConversation(userId, otherUserId);
        for (Message m : conversation) {
            if (m.getReceiver() != null && m.getReceiver().getId().equals(userId) && !m.isRead()) {
                m.setRead(true);
                messageRepository.save(m);
            }
        }
    }

    public long unreadPrivateCount(Long userId) {
        return messageRepository.countByReceiverIdAndIsReadFalse(userId);
    }

    private MessageResponse toResponse(Message m) {
        return new MessageResponse(
                m.getId(), m.getSender().getId(), m.getSender().getFullName(),
                m.getReceiver() != null ? m.getReceiver().getId() : null,
                m.getReceiver() != null ? m.getReceiver().getFullName() : null,
                m.getContent(), m.isRead(), m.getType().name(), m.getCreatedAt()
        );
    }
}

package com.roommate.management.repository;

import com.roommate.management.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByRoomIdAndTypeOrderByCreatedAtAsc(Long roomId, com.roommate.management.entity.enums.MessageType type);

    @Query("SELECT m FROM Message m WHERE m.type = 'PRIVATE' AND " +
           "((m.sender.id = :userA AND m.receiver.id = :userB) OR (m.sender.id = :userB AND m.receiver.id = :userA)) " +
           "ORDER BY m.createdAt ASC")
    List<Message> findConversation(@Param("userA") Long userA, @Param("userB") Long userB);

    long countByReceiverIdAndIsReadFalse(Long receiverId);
}

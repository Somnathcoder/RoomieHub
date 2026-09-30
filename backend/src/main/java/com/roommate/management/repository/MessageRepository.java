package com.roommate.management.repository;

import com.roommate.management.entity.Message;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    // sender/receiver fetched in the same query (receiver is nullable for PUBLIC messages,
    // hence @EntityGraph rather than an inner JOIN FETCH) - avoids 1-2 lazy loads per message
    // when mapping the whole chat to a response list.
    @EntityGraph(attributePaths = {"sender", "receiver"})
    List<Message> findByRoomIdAndTypeOrderByCreatedAtAsc(Long roomId, com.roommate.management.entity.enums.MessageType type);

    @Query("SELECT m FROM Message m LEFT JOIN FETCH m.sender LEFT JOIN FETCH m.receiver WHERE m.type = 'PRIVATE' AND " +
           "((m.sender.id = :userA AND m.receiver.id = :userB) OR (m.sender.id = :userB AND m.receiver.id = :userA)) " +
           "ORDER BY m.createdAt ASC")
    List<Message> findConversation(@Param("userA") Long userA, @Param("userB") Long userB);

    long countByReceiverIdAndIsReadFalse(Long receiverId);
}

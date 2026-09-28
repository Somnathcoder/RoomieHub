package com.roommate.management.repository;

import com.roommate.management.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findByRoomIdOrderByCreatedAtDesc(Long roomId);
}

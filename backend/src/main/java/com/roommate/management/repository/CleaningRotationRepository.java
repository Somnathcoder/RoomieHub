package com.roommate.management.repository;

import com.roommate.management.entity.CleaningRotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CleaningRotationRepository extends JpaRepository<CleaningRotation, Long> {
    List<CleaningRotation> findByRoomId(Long roomId);
    List<CleaningRotation> findByActiveTrue();
}

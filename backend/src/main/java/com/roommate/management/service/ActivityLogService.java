package com.roommate.management.service;

import com.roommate.management.dto.response.ActivityLogResponse;
import com.roommate.management.entity.ActivityLog;
import com.roommate.management.entity.Room;
import com.roommate.management.entity.User;
import com.roommate.management.entity.enums.ActivityModule;
import com.roommate.management.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    @Transactional
    public void log(Room room, User user, ActivityModule module, String action, Long referenceId, String description) {
        ActivityLog log = ActivityLog.builder()
                .room(room)
                .user(user)
                .module(module)
                .action(action)
                .referenceId(referenceId)
                .description(description)
                .build();
        activityLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<ActivityLogResponse> getRoomLogs(Long roomId, Long userId, ActivityModule module, LocalDate from, LocalDate to) {
        Specification<ActivityLog> spec = (root, query, cb) -> cb.equal(root.get("room").get("id"), roomId);
        if (userId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("user").get("id"), userId));
        }
        if (module != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("module"), module));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to.atTime(23, 59, 59)));
        }
        return activityLogRepository.findAll(spec, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
                .stream().map(this::toResponse).toList();
    }

    private ActivityLogResponse toResponse(ActivityLog log) {
        return new ActivityLogResponse(
                log.getId(),
                log.getUser().getFullName(),
                log.getAction(),
                log.getModule().name(),
                log.getReferenceId(),
                log.getDescription(),
                log.getCreatedAt()
        );
    }
}

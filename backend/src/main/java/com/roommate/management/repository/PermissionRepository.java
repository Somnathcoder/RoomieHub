package com.roommate.management.repository;

import com.roommate.management.entity.Permission;
import com.roommate.management.entity.enums.PermissionCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByCode(PermissionCode code);
}

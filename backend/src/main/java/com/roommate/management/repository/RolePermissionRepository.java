package com.roommate.management.repository;

import com.roommate.management.entity.RolePermission;
import com.roommate.management.entity.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    List<RolePermission> findByRoleName(RoleType roleName);
}

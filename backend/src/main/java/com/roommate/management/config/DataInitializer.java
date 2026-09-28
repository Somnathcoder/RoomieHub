package com.roommate.management.config;

import com.roommate.management.entity.Permission;
import com.roommate.management.entity.Role;
import com.roommate.management.entity.RolePermission;
import com.roommate.management.entity.enums.PermissionCode;
import com.roommate.management.entity.enums.RoleType;
import com.roommate.management.repository.PermissionRepository;
import com.roommate.management.repository.RolePermissionRepository;
import com.roommate.management.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seeds the roles / permissions / role_permissions reference tables on
 * startup. Safe to run every boot: everything is looked up by natural key
 * before insert, so re-running never creates duplicates.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Override
    @Transactional
    public void run(String... args) {
        for (RoleType roleType : RoleType.values()) {
            roleRepository.findByName(roleType).orElseGet(() -> roleRepository.save(Role.builder().name(roleType).build()));
        }

        for (PermissionCode code : PermissionCode.values()) {
            permissionRepository.findByCode(code).orElseGet(() ->
                    permissionRepository.save(Permission.builder().code(code).description(describe(code)).build()));
        }

        Role adminRole = roleRepository.findByName(RoleType.ADMIN).orElseThrow();
        List<RolePermission> adminMappings = rolePermissionRepository.findByRoleName(RoleType.ADMIN);
        if (adminMappings.size() < PermissionCode.values().length) {
            for (PermissionCode code : PermissionCode.values()) {
                Permission permission = permissionRepository.findByCode(code).orElseThrow();
                boolean exists = adminMappings.stream().anyMatch(m -> m.getPermission().getCode() == code);
                if (!exists) {
                    rolePermissionRepository.save(RolePermission.builder().role(adminRole).permission(permission).build());
                }
            }
        }
    }

    private String describe(PermissionCode code) {
        return switch (code) {
            case ADD_MEMBER -> "Add new roommates to the room";
            case REMOVE_MEMBER -> "Deactivate or remove roommates";
            case MANAGE_EXPENSE -> "Add, edit, and delete expenses";
            case APPROVE_EXPENSE -> "Approve or reject pending expenses";
            case MANAGE_SETTLEMENT -> "Verify and manage settlements";
            case MANAGE_BILL -> "Create and manage bills";
            case MANAGE_TASK -> "Create and manage tasks";
            case MANAGE_GROCERY -> "Manage the shopping list";
            case MANAGE_INVENTORY -> "Manage room inventory";
            case MANAGE_ANNOUNCEMENT -> "Post and manage announcements";
            case MANAGE_CLEANING -> "Manage cleaning schedules and rotation";
            case MANAGE_ISSUE -> "Update reported issue status";
            case MANAGE_POLL -> "Create and close polls";
            case VIEW_ACTIVITY_LOG -> "View the room activity log";
        };
    }
}

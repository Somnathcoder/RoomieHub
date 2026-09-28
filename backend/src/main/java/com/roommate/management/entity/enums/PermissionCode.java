package com.roommate.management.entity.enums;

/**
 * Fine-grained permission codes. ADMIN implicitly has all of these.
 * MODERATOR has whatever subset the room ADMIN grants via member_permissions.
 */
public enum PermissionCode {
    ADD_MEMBER,
    REMOVE_MEMBER,
    MANAGE_EXPENSE,
    APPROVE_EXPENSE,
    MANAGE_SETTLEMENT,
    MANAGE_BILL,
    MANAGE_TASK,
    MANAGE_GROCERY,
    MANAGE_INVENTORY,
    MANAGE_ANNOUNCEMENT,
    MANAGE_CLEANING,
    MANAGE_ISSUE,
    MANAGE_POLL,
    VIEW_ACTIVITY_LOG
}

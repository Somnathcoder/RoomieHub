-- =====================================================================
-- Roommate Management System - MySQL Schema
-- =====================================================================
-- Run this once against an empty database, e.g.:
--   mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS roommate_management"
--   mysql -u root -p roommate_management < schema.sql
--
-- The application is configured with spring.jpa.hibernate.ddl-auto=none,
-- so this file (not Hibernate) is the single source of truth for the schema.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS roommate_management
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE roommate_management;

SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------
-- users
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS users;
CREATE TABLE users (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name           VARCHAR(120)  NOT NULL,
    email               VARCHAR(150)  NOT NULL,
    password            VARCHAR(255)  NOT NULL,
    mobile_number       VARCHAR(20)   NULL,
    profile_photo_url   VARCHAR(500)  NULL,
    id_proof_url        VARCHAR(500)  NULL,
    enabled             TINYINT(1)    NOT NULL DEFAULT 1,
    reset_token         VARCHAR(255)  NULL,
    reset_token_expiry  DATETIME      NULL,
    created_at          DATETIME      NOT NULL,
    updated_at          DATETIME      NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- rooms
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS rooms;
CREATE TABLE rooms (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_name    VARCHAR(150)  NOT NULL,
    address      VARCHAR(500)  NOT NULL,
    created_by   BIGINT        NOT NULL,
    status       VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at   DATETIME      NOT NULL,
    updated_at   DATETIME      NULL,
    CONSTRAINT fk_rooms_created_by FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- roles / permissions / role_permissions  (reference data, seeded on boot)
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS roles;
CREATE TABLE roles (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(30) NOT NULL,
    created_at  DATETIME NOT NULL,
    updated_at  DATETIME NULL,
    CONSTRAINT uk_roles_name UNIQUE (name)
) ENGINE=InnoDB;

DROP TABLE IF EXISTS permissions;
CREATE TABLE permissions (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    code         VARCHAR(40)  NOT NULL,
    description  VARCHAR(255) NULL,
    created_at   DATETIME NOT NULL,
    updated_at   DATETIME NULL,
    CONSTRAINT uk_permissions_code UNIQUE (code)
) ENGINE=InnoDB;

DROP TABLE IF EXISTS role_permissions;
CREATE TABLE role_permissions (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id        BIGINT NOT NULL,
    permission_id  BIGINT NOT NULL,
    created_at     DATETIME NOT NULL,
    updated_at     DATETIME NULL,
    CONSTRAINT uk_role_permission UNIQUE (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles(id),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions(id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- room_members  (the membership of a user inside a room, with a role)
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS room_members;
CREATE TABLE room_members (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id       BIGINT       NOT NULL,
    user_id       BIGINT       NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    room_number   VARCHAR(30)  NULL,
    bed_number    VARCHAR(30)  NULL,
    joining_date  DATE         NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    DATETIME     NOT NULL,
    updated_at    DATETIME     NULL,
    CONSTRAINT uk_room_user UNIQUE (room_id, user_id),
    CONSTRAINT fk_room_members_room FOREIGN KEY (room_id) REFERENCES rooms(id),
    CONSTRAINT fk_room_members_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- member_permissions (per-member overrides, mainly used for MODERATORs)
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS member_permissions;
CREATE TABLE member_permissions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_member_id  BIGINT      NOT NULL,
    permission_id   BIGINT      NOT NULL,
    granted         TINYINT(1)  NOT NULL DEFAULT 1,
    created_at      DATETIME NOT NULL,
    updated_at      DATETIME NULL,
    CONSTRAINT uk_member_permission UNIQUE (room_member_id, permission_id),
    CONSTRAINT fk_member_permissions_member FOREIGN KEY (room_member_id) REFERENCES room_members(id),
    CONSTRAINT fk_member_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions(id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- recurring_expenses (created before expenses: expenses references it)
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS recurring_expenses;
CREATE TABLE recurring_expenses (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id        BIGINT        NOT NULL,
    title          VARCHAR(150)  NOT NULL,
    category       VARCHAR(30)   NOT NULL,
    amount         DECIMAL(12,2) NOT NULL,
    frequency      VARCHAR(20)   NOT NULL,
    split_type     VARCHAR(20)   NOT NULL DEFAULT 'EQUAL',
    start_date     DATE          NOT NULL,
    next_due_date  DATE          NOT NULL,
    paid_by        BIGINT        NOT NULL,
    created_by     BIGINT        NOT NULL,
    active         TINYINT(1)    NOT NULL DEFAULT 1,
    created_at     DATETIME NOT NULL,
    updated_at     DATETIME NULL,
    CONSTRAINT fk_recurring_room FOREIGN KEY (room_id) REFERENCES rooms(id),
    CONSTRAINT fk_recurring_paid_by FOREIGN KEY (paid_by) REFERENCES room_members(id),
    CONSTRAINT fk_recurring_created_by FOREIGN KEY (created_by) REFERENCES room_members(id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- expenses
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS expenses;
CREATE TABLE expenses (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id                BIGINT        NOT NULL,
    title                  VARCHAR(150)  NOT NULL,
    description            VARCHAR(1000) NULL,
    total_amount           DECIMAL(12,2) NOT NULL,
    category               VARCHAR(30)   NOT NULL,
    paid_by                BIGINT        NOT NULL,
    expense_date           DATE          NOT NULL,
    receipt_photo_url      VARCHAR(500)  NULL,
    split_type             VARCHAR(20)   NOT NULL,
    status                 VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    created_by             BIGINT        NOT NULL,
    approved_by            BIGINT        NULL,
    approved_at            DATETIME      NULL,
    rejection_reason       VARCHAR(500)  NULL,
    recurring_expense_id   BIGINT        NULL,
    created_at             DATETIME NOT NULL,
    updated_at             DATETIME NULL,
    CONSTRAINT fk_expenses_room FOREIGN KEY (room_id) REFERENCES rooms(id),
    CONSTRAINT fk_expenses_paid_by FOREIGN KEY (paid_by) REFERENCES room_members(id),
    CONSTRAINT fk_expenses_created_by FOREIGN KEY (created_by) REFERENCES room_members(id),
    CONSTRAINT fk_expenses_approved_by FOREIGN KEY (approved_by) REFERENCES room_members(id),
    CONSTRAINT fk_expenses_recurring FOREIGN KEY (recurring_expense_id) REFERENCES recurring_expenses(id),
    INDEX idx_expenses_room_date (room_id, expense_date),
    INDEX idx_expenses_status (status)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- expense_splits
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS expense_splits;
CREATE TABLE expense_splits (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    expense_id      BIGINT        NOT NULL,
    room_member_id  BIGINT        NOT NULL,
    share_amount    DECIMAL(12,2) NOT NULL,
    created_at      DATETIME NOT NULL,
    updated_at      DATETIME NULL,
    CONSTRAINT uk_expense_member UNIQUE (expense_id, room_member_id),
    CONSTRAINT fk_splits_expense FOREIGN KEY (expense_id) REFERENCES expenses(id) ON DELETE CASCADE,
    CONSTRAINT fk_splits_member FOREIGN KEY (room_member_id) REFERENCES room_members(id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- settlements
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS settlements;
CREATE TABLE settlements (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id             BIGINT        NOT NULL,
    expense_id          BIGINT        NULL,
    from_member_id      BIGINT        NOT NULL,
    to_member_id        BIGINT        NOT NULL,
    amount              DECIMAL(12,2) NOT NULL,
    status              VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    payment_date        DATE          NULL,
    verified_by_admin   TINYINT(1)    NOT NULL DEFAULT 0,
    verified_by         BIGINT        NULL,
    created_at          DATETIME NOT NULL,
    updated_at          DATETIME NULL,
    CONSTRAINT fk_settlements_room FOREIGN KEY (room_id) REFERENCES rooms(id),
    CONSTRAINT fk_settlements_expense FOREIGN KEY (expense_id) REFERENCES expenses(id) ON DELETE CASCADE,
    CONSTRAINT fk_settlements_from FOREIGN KEY (from_member_id) REFERENCES room_members(id),
    CONSTRAINT fk_settlements_to FOREIGN KEY (to_member_id) REFERENCES room_members(id),
    CONSTRAINT fk_settlements_verified_by FOREIGN KEY (verified_by) REFERENCES room_members(id),
    INDEX idx_settlements_room_status (room_id, status)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- shopping_items
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS shopping_items;
CREATE TABLE shopping_items (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id             BIGINT        NOT NULL,
    item_name           VARCHAR(150)  NOT NULL,
    quantity            VARCHAR(50)   NULL,
    estimated_amount    DECIMAL(12,2) NULL,
    added_by            BIGINT        NOT NULL,
    status              VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    item_photo_url      VARCHAR(500)  NULL,
    bill_photo_url      VARCHAR(500)  NULL,
    purchase_date       DATE          NULL,
    linked_expense_id   BIGINT        NULL,
    created_at          DATETIME NOT NULL,
    updated_at          DATETIME NULL,
    CONSTRAINT fk_shopping_room FOREIGN KEY (room_id) REFERENCES rooms(id),
    CONSTRAINT fk_shopping_added_by FOREIGN KEY (added_by) REFERENCES room_members(id),
    CONSTRAINT fk_shopping_expense FOREIGN KEY (linked_expense_id) REFERENCES expenses(id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- bills
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS bills;
CREATE TABLE bills (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id          BIGINT        NOT NULL,
    title            VARCHAR(150)  NOT NULL,
    amount           DECIMAL(12,2) NOT NULL,
    amount_paid      DECIMAL(12,2) NOT NULL DEFAULT 0,
    due_date         DATE          NOT NULL,
    paid_by          BIGINT        NULL,
    category         VARCHAR(30)   NOT NULL,
    bill_photo_url   VARCHAR(500)  NULL,
    status           VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    created_by       BIGINT        NOT NULL,
    created_at       DATETIME NOT NULL,
    updated_at       DATETIME NULL,
    CONSTRAINT fk_bills_room FOREIGN KEY (room_id) REFERENCES rooms(id),
    CONSTRAINT fk_bills_paid_by FOREIGN KEY (paid_by) REFERENCES room_members(id),
    CONSTRAINT fk_bills_created_by FOREIGN KEY (created_by) REFERENCES room_members(id),
    INDEX idx_bills_room_due (room_id, due_date)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- tasks (general chores / to-dos, distinct from cleaning_schedules)
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS tasks;
CREATE TABLE tasks (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id       BIGINT        NOT NULL,
    title         VARCHAR(150)  NOT NULL,
    description   VARCHAR(1000) NULL,
    assigned_to   BIGINT        NOT NULL,
    due_date      DATE          NULL,
    status        VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    priority      VARCHAR(20)   NOT NULL DEFAULT 'MEDIUM',
    created_by    BIGINT        NOT NULL,
    completed_at  DATETIME      NULL,
    created_at    DATETIME NOT NULL,
    updated_at    DATETIME NULL,
    CONSTRAINT fk_tasks_room FOREIGN KEY (room_id) REFERENCES rooms(id),
    CONSTRAINT fk_tasks_assigned_to FOREIGN KEY (assigned_to) REFERENCES room_members(id),
    CONSTRAINT fk_tasks_created_by FOREIGN KEY (created_by) REFERENCES room_members(id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- cleaning_rotations (created before cleaning_schedules, which reference it)
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS cleaning_rotations;
CREATE TABLE cleaning_rotations (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id           BIGINT       NOT NULL,
    task_type         VARCHAR(30)  NOT NULL,
    member_order_csv  VARCHAR(500) NOT NULL,
    current_index     INT          NOT NULL DEFAULT 0,
    interval_days     INT          NOT NULL DEFAULT 7,
    start_date        DATE         NOT NULL,
    active            TINYINT(1)   NOT NULL DEFAULT 1,
    created_at        DATETIME NOT NULL,
    updated_at        DATETIME NULL,
    CONSTRAINT fk_rotations_room FOREIGN KEY (room_id) REFERENCES rooms(id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- cleaning_schedules
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS cleaning_schedules;
CREATE TABLE cleaning_schedules (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id                     BIGINT        NOT NULL,
    title                       VARCHAR(150)  NOT NULL,
    cleaning_date               DATE          NOT NULL,
    cleaning_time               TIME          NULL,
    assigned_to                 BIGINT        NOT NULL,
    task_type                   VARCHAR(30)   NOT NULL,
    description                 VARCHAR(1000) NULL,
    status                      VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    rotation_id                 BIGINT        NULL,
    completed_at                DATETIME      NULL,
    reminder_day_before_sent    TINYINT(1)    NOT NULL DEFAULT 0,
    reminder_on_day_sent        TINYINT(1)    NOT NULL DEFAULT 0,
    reminder_overdue_sent       TINYINT(1)    NOT NULL DEFAULT 0,
    created_at                  DATETIME NOT NULL,
    updated_at                  DATETIME NULL,
    CONSTRAINT fk_cleaning_room FOREIGN KEY (room_id) REFERENCES rooms(id),
    CONSTRAINT fk_cleaning_assigned_to FOREIGN KEY (assigned_to) REFERENCES room_members(id),
    CONSTRAINT fk_cleaning_rotation FOREIGN KEY (rotation_id) REFERENCES cleaning_rotations(id),
    INDEX idx_cleaning_room_date (room_id, cleaning_date)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- announcements
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS announcements;
CREATE TABLE announcements (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id      BIGINT        NOT NULL,
    title        VARCHAR(150)  NOT NULL,
    message      VARCHAR(2000) NOT NULL,
    created_by   BIGINT        NOT NULL,
    image_url    VARCHAR(500)  NULL,
    status       VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at   DATETIME NOT NULL,
    updated_at   DATETIME NULL,
    CONSTRAINT fk_announcements_room FOREIGN KEY (room_id) REFERENCES rooms(id),
    CONSTRAINT fk_announcements_created_by FOREIGN KEY (created_by) REFERENCES room_members(id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- messages (public room chat + private 1:1 messages)
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS messages;
CREATE TABLE messages (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id      BIGINT        NOT NULL,
    sender_id    BIGINT        NOT NULL,
    receiver_id  BIGINT        NULL,
    type         VARCHAR(20)   NOT NULL,
    content      VARCHAR(2000) NOT NULL,
    is_read      TINYINT(1)    NOT NULL DEFAULT 0,
    created_at   DATETIME NOT NULL,
    updated_at   DATETIME NULL,
    CONSTRAINT fk_messages_room FOREIGN KEY (room_id) REFERENCES rooms(id),
    CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users(id),
    CONSTRAINT fk_messages_receiver FOREIGN KEY (receiver_id) REFERENCES users(id),
    INDEX idx_messages_room_type (room_id, type),
    INDEX idx_messages_sender_receiver (sender_id, receiver_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- notifications
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS notifications;
CREATE TABLE notifications (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT        NOT NULL,
    room_id       BIGINT        NULL,
    type          VARCHAR(40)   NOT NULL,
    title         VARCHAR(150)  NOT NULL,
    message       VARCHAR(1000) NOT NULL,
    reference_id  BIGINT        NULL,
    is_read       TINYINT(1)    NOT NULL DEFAULT 0,
    created_at    DATETIME NOT NULL,
    updated_at    DATETIME NULL,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_notifications_room FOREIGN KEY (room_id) REFERENCES rooms(id),
    INDEX idx_notifications_user_read (user_id, is_read)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- polls / poll_options / poll_votes
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS polls;
CREATE TABLE polls (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id      BIGINT        NOT NULL,
    question     VARCHAR(500)  NOT NULL,
    created_by   BIGINT        NOT NULL,
    status       VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
    closes_at    DATETIME      NULL,
    created_at   DATETIME NOT NULL,
    updated_at   DATETIME NULL,
    CONSTRAINT fk_polls_room FOREIGN KEY (room_id) REFERENCES rooms(id),
    CONSTRAINT fk_polls_created_by FOREIGN KEY (created_by) REFERENCES room_members(id)
) ENGINE=InnoDB;

DROP TABLE IF EXISTS poll_options;
CREATE TABLE poll_options (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    poll_id       BIGINT        NOT NULL,
    option_text   VARCHAR(255)  NOT NULL,
    created_at    DATETIME NOT NULL,
    updated_at    DATETIME NULL,
    CONSTRAINT fk_poll_options_poll FOREIGN KEY (poll_id) REFERENCES polls(id) ON DELETE CASCADE
) ENGINE=InnoDB;

DROP TABLE IF EXISTS poll_votes;
CREATE TABLE poll_votes (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    poll_id     BIGINT NOT NULL,
    option_id   BIGINT NOT NULL,
    voter_id    BIGINT NOT NULL,
    created_at  DATETIME NOT NULL,
    updated_at  DATETIME NULL,
    CONSTRAINT uk_poll_voter UNIQUE (poll_id, voter_id),
    CONSTRAINT fk_poll_votes_poll FOREIGN KEY (poll_id) REFERENCES polls(id) ON DELETE CASCADE,
    CONSTRAINT fk_poll_votes_option FOREIGN KEY (option_id) REFERENCES poll_options(id) ON DELETE CASCADE,
    CONSTRAINT fk_poll_votes_voter FOREIGN KEY (voter_id) REFERENCES room_members(id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- inventory_items
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS inventory_items;
CREATE TABLE inventory_items (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id           BIGINT        NOT NULL,
    item_name         VARCHAR(150)  NOT NULL,
    quantity          INT           NOT NULL DEFAULT 1,
    added_date        DATE          NOT NULL,
    added_by          BIGINT        NOT NULL,
    photo_url         VARCHAR(500)  NULL,
    condition_status  VARCHAR(20)   NOT NULL DEFAULT 'WORKING',
    notes             VARCHAR(1000) NULL,
    created_at        DATETIME NOT NULL,
    updated_at        DATETIME NULL,
    CONSTRAINT fk_inventory_room FOREIGN KEY (room_id) REFERENCES rooms(id),
    CONSTRAINT fk_inventory_added_by FOREIGN KEY (added_by) REFERENCES room_members(id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- issues
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS issues;
CREATE TABLE issues (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id       BIGINT        NOT NULL,
    title         VARCHAR(150)  NOT NULL,
    description   VARCHAR(1000) NULL,
    photo_url     VARCHAR(500)  NULL,
    priority      VARCHAR(20)   NOT NULL DEFAULT 'MEDIUM',
    reported_by   BIGINT        NOT NULL,
    status        VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
    resolved_at   DATETIME      NULL,
    created_at    DATETIME NOT NULL,
    updated_at    DATETIME NULL,
    CONSTRAINT fk_issues_room FOREIGN KEY (room_id) REFERENCES rooms(id),
    CONSTRAINT fk_issues_reported_by FOREIGN KEY (reported_by) REFERENCES room_members(id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- activity_logs
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS activity_logs;
CREATE TABLE activity_logs (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id       BIGINT        NOT NULL,
    user_id       BIGINT        NOT NULL,
    action        VARCHAR(150)  NOT NULL,
    module        VARCHAR(30)   NOT NULL,
    reference_id  BIGINT        NULL,
    description   VARCHAR(1000) NULL,
    created_at    DATETIME NOT NULL,
    updated_at    DATETIME NULL,
    CONSTRAINT fk_activity_room FOREIGN KEY (room_id) REFERENCES rooms(id),
    CONSTRAINT fk_activity_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_activity_room_created (room_id, created_at)
) ENGINE=InnoDB;

SET FOREIGN_KEY_CHECKS = 1;

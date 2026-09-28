-- =====================================================================
-- Roommate Management System - Sample Development Data
-- =====================================================================
-- Run AFTER schema.sql, against the same database:
--   mysql -u root -p roommate_management < seed-data.sql
--
-- All sample people below are fictional. Every seeded account uses the
-- SAME development password: password123
-- (bcrypt hash generated with cost factor 10 - verified compatible with
-- Spring Security's BCryptPasswordEncoder)
-- =====================================================================

USE roommate_management;
SET FOREIGN_KEY_CHECKS = 0;
SET @now = '2026-01-01 09:00:00';
SET @pw = '$2b$10$0dthdRxWwtu.7sJz8mKdbOrId/FLriq9T5LpAC9pktLpi0dA3WwH.'; -- password123

-- ---------------------------------------------------------------------
-- roles / permissions / role_permissions
-- (the application also seeds these automatically on first boot; this is
--  here so the schema is fully populated if you inspect the DB directly)
-- ---------------------------------------------------------------------
INSERT INTO roles (id, name, created_at, updated_at) VALUES
  (1, 'ADMIN', @now, @now),
  (2, 'MEMBER', @now, @now),
  (3, 'MODERATOR', @now, @now);

INSERT INTO permissions (id, code, description, created_at, updated_at) VALUES
  (1,  'ADD_MEMBER',          'Add new roommates to the room',            @now, @now),
  (2,  'REMOVE_MEMBER',       'Deactivate or remove roommates',           @now, @now),
  (3,  'MANAGE_EXPENSE',      'Add, edit, and delete expenses',           @now, @now),
  (4,  'APPROVE_EXPENSE',     'Approve or reject pending expenses',       @now, @now),
  (5,  'MANAGE_SETTLEMENT',   'Verify and manage settlements',            @now, @now),
  (6,  'MANAGE_BILL',         'Create and manage bills',                  @now, @now),
  (7,  'MANAGE_TASK',         'Create and manage tasks',                  @now, @now),
  (8,  'MANAGE_GROCERY',      'Manage the shopping list',                 @now, @now),
  (9,  'MANAGE_INVENTORY',    'Manage room inventory',                    @now, @now),
  (10, 'MANAGE_ANNOUNCEMENT', 'Post and manage announcements',            @now, @now),
  (11, 'MANAGE_CLEANING',     'Manage cleaning schedules and rotation',   @now, @now),
  (12, 'MANAGE_ISSUE',        'Update reported issue status',             @now, @now),
  (13, 'MANAGE_POLL',         'Create and close polls',                   @now, @now),
  (14, 'VIEW_ACTIVITY_LOG',   'View the room activity log',               @now, @now);

INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT 1, id, @now, @now FROM permissions; -- ADMIN implicitly has every permission

-- ---------------------------------------------------------------------
-- users  (all passwords: password123)
-- ---------------------------------------------------------------------
INSERT INTO users (id, full_name, email, password, mobile_number, enabled, created_at, updated_at) VALUES
  (1, 'Aditi Sharma', 'admin@roommates.dev',     @pw, '9800000001', 1, @now, @now),
  (2, 'Rahul Verma',  'rahul@roommates.dev',     @pw, '9800000002', 1, @now, @now),
  (3, 'Akash Gupta',  'akash@roommates.dev',     @pw, '9800000003', 1, @now, @now),
  (4, 'Priya Nair',   'priya@roommates.dev',     @pw, '9800000004', 1, @now, @now);

-- ---------------------------------------------------------------------
-- rooms
-- ---------------------------------------------------------------------
INSERT INTO rooms (id, room_name, address, created_by, status, created_at, updated_at) VALUES
  (1, 'Sunrise PG - Flat 4B', '221 MG Road, Pune, Maharashtra', 1, 'ACTIVE', @now, @now);

-- ---------------------------------------------------------------------
-- room_members  (Aditi = ADMIN, Rahul & Akash = MEMBER, Priya = MODERATOR)
-- ---------------------------------------------------------------------
INSERT INTO room_members (id, room_id, user_id, role, room_number, bed_number, joining_date, status, created_at, updated_at) VALUES
  (1, 1, 1, 'ADMIN',     '4B', 'A1', '2025-11-01', 'ACTIVE', @now, @now),
  (2, 1, 2, 'MEMBER',    '4B', 'A2', '2025-11-05', 'ACTIVE', @now, @now),
  (3, 1, 3, 'MEMBER',    '4B', 'B1', '2025-12-01', 'ACTIVE', @now, @now),
  (4, 1, 4, 'MODERATOR', '4B', 'B2', '2025-12-10', 'ACTIVE', @now, @now);

-- Priya (moderator) is granted task + grocery management by the admin
INSERT INTO member_permissions (room_member_id, permission_id, granted, created_at, updated_at) VALUES
  (4, 7, 1, @now, @now),
  (4, 8, 1, @now, @now);

-- ---------------------------------------------------------------------
-- recurring_expenses
-- ---------------------------------------------------------------------
INSERT INTO recurring_expenses (id, room_id, title, category, amount, frequency, split_type, start_date, next_due_date, paid_by, created_by, active, created_at, updated_at) VALUES
  (1, 1, 'Monthly Rent', 'OTHER', 12000.00, 'MONTHLY', 'EQUAL', '2026-01-01', '2026-02-01', 1, 1, 1, @now, @now);

-- ---------------------------------------------------------------------
-- expenses + expense_splits
-- ---------------------------------------------------------------------
INSERT INTO expenses (id, room_id, title, description, total_amount, category, paid_by, expense_date, split_type, status, created_by, approved_by, approved_at, created_at, updated_at) VALUES
  (1, 1, 'Grocery Run', 'Weekly groceries from the supermarket', 2000.00, 'GROCERY', 2, '2026-01-10', 'EQUAL',  'APPROVED', 2, 1, @now, @now, @now),
  (2, 1, 'Electricity Bill Split', 'January electricity bill shared unevenly by usage', 1800.00, 'ELECTRICITY', 1, '2026-01-12', 'CUSTOM', 'PENDING', 3, NULL, NULL, @now, @now);

INSERT INTO expense_splits (expense_id, room_member_id, share_amount, created_at, updated_at) VALUES
  (1, 1, 500.00, @now, @now),
  (1, 2, 500.00, @now, @now),
  (1, 3, 500.00, @now, @now),
  (1, 4, 500.00, @now, @now),
  (2, 1, 600.00, @now, @now),
  (2, 2, 400.00, @now, @now),
  (2, 3, 400.00, @now, @now),
  (2, 4, 400.00, @now, @now);

-- ---------------------------------------------------------------------
-- settlements  (generated only for the APPROVED expense: id 1, paid by Rahul)
-- ---------------------------------------------------------------------
INSERT INTO settlements (room_id, expense_id, from_member_id, to_member_id, amount, status, created_at, updated_at) VALUES
  (1, 1, 1, 2, 500.00, 'PENDING', @now, @now),
  (1, 1, 3, 2, 500.00, 'PENDING', @now, @now),
  (1, 1, 4, 2, 500.00, 'PENDING', @now, @now);

-- ---------------------------------------------------------------------
-- shopping_items
-- ---------------------------------------------------------------------
INSERT INTO shopping_items (room_id, item_name, quantity, estimated_amount, added_by, status, purchase_date, created_at, updated_at) VALUES
  (1, 'Milk',      '2 litres', 120.00, 3, 'PENDING',   NULL,        @now, @now),
  (1, 'Dish Soap', '1 bottle',  90.00, 4, 'PURCHASED', '2026-01-08', @now, @now);

-- ---------------------------------------------------------------------
-- bills
-- ---------------------------------------------------------------------
INSERT INTO bills (room_id, title, amount, amount_paid, due_date, paid_by, category, status, created_by, created_at, updated_at) VALUES
  (1, 'Wifi Bill - January', 999.00, 0.00,   '2026-01-20', NULL, 'WIFI', 'PENDING', 1, @now, @now),
  (1, 'Gas Cylinder Refill', 850.00, 850.00, '2026-01-05', 1,    'GAS',  'PAID',    1, @now, @now);

-- ---------------------------------------------------------------------
-- tasks
-- ---------------------------------------------------------------------
INSERT INTO tasks (room_id, title, description, assigned_to, due_date, status, priority, created_by, created_at, updated_at) VALUES
  (1, 'Take out the trash',   'Every Thursday evening before collection', 3, '2026-01-16', 'PENDING',     'LOW',  1, @now, @now),
  (1, 'Fix the leaking tap',  'Kitchen tap has been dripping',            2, '2026-01-18', 'IN_PROGRESS', 'HIGH', 1, @now, @now);

-- ---------------------------------------------------------------------
-- cleaning_rotations + cleaning_schedules
-- ---------------------------------------------------------------------
INSERT INTO cleaning_rotations (id, room_id, task_type, member_order_csv, current_index, interval_days, start_date, active, created_at, updated_at) VALUES
  (1, 1, 'KITCHEN', '1,2,3,4', 1, 7, '2026-01-05', 1, @now, @now);

INSERT INTO cleaning_schedules (room_id, title, cleaning_date, cleaning_time, assigned_to, task_type, status, rotation_id, completed_at, created_at, updated_at) VALUES
  (1, 'Kitchen Cleaning', '2026-01-05', '10:00:00', 1, 'KITCHEN', 'COMPLETED', 1, '2026-01-05 11:00:00', @now, @now),
  (1, 'Kitchen Cleaning', '2026-01-12', '10:00:00', 2, 'KITCHEN', 'PENDING',   1, NULL,                  @now, @now);

-- ---------------------------------------------------------------------
-- announcements
-- ---------------------------------------------------------------------
INSERT INTO announcements (room_id, title, message, created_by, status, created_at, updated_at) VALUES
  (1, 'Welcome to Sunrise PG', 'Please keep common areas clean and pay your share of bills by the 5th of every month.', 1, 'ACTIVE', @now, @now);

-- ---------------------------------------------------------------------
-- messages
-- ---------------------------------------------------------------------
INSERT INTO messages (room_id, sender_id, receiver_id, type, content, is_read, created_at, updated_at) VALUES
  (1, 1, NULL, 'PUBLIC',  'Reminder: rent is due on the 1st of every month.', 1, @now, @now),
  (1, 2, 1,    'PRIVATE', 'Hey, can I get the wifi password again?',          0, @now, @now);

-- ---------------------------------------------------------------------
-- notifications
-- ---------------------------------------------------------------------
INSERT INTO notifications (user_id, room_id, type, title, message, reference_id, is_read, created_at, updated_at) VALUES
  (2, 1, 'NEW_ANNOUNCEMENT',  'Welcome to Sunrise PG',    'Please keep common areas clean and pay your share of bills by the 5th of every month.', 1, 1, @now, @now),
  (1, 1, 'PAYMENT_REMINDER',  'Settlement pending',       'Akash Gupta and 2 others owe you for Grocery Run.',                                     1, 0, @now, @now);

-- ---------------------------------------------------------------------
-- polls / poll_options / poll_votes
-- ---------------------------------------------------------------------
INSERT INTO polls (id, room_id, question, created_by, status, closes_at, created_at, updated_at) VALUES
  (1, 1, 'Should we hire a weekly maid?', 1, 'OPEN', '2026-02-01 00:00:00', @now, @now);

INSERT INTO poll_options (id, poll_id, option_text, created_at, updated_at) VALUES
  (1, 1, 'Yes', @now, @now),
  (2, 1, 'No',  @now, @now);

INSERT INTO poll_votes (poll_id, option_id, voter_id, created_at, updated_at) VALUES
  (1, 1, 2, @now, @now),
  (1, 2, 3, @now, @now);

-- ---------------------------------------------------------------------
-- inventory_items
-- ---------------------------------------------------------------------
INSERT INTO inventory_items (room_id, item_name, quantity, added_date, added_by, condition_status, notes, created_at, updated_at) VALUES
  (1, 'Gas Cylinder',     1, '2026-01-01', 1, 'WORKING', NULL,                              @now, @now),
  (1, 'Water Purifier',   1, '2026-01-01', 1, 'DAMAGED', 'Filter needs replacement',         @now, @now);

-- ---------------------------------------------------------------------
-- issues
-- ---------------------------------------------------------------------
INSERT INTO issues (room_id, title, description, priority, reported_by, status, created_at, updated_at) VALUES
  (1, 'Bathroom tap leaking', 'The common bathroom tap has been leaking for two days.', 'HIGH', 3, 'OPEN', @now, @now);

-- ---------------------------------------------------------------------
-- activity_logs (a few, illustrating the audit trail)
-- ---------------------------------------------------------------------
INSERT INTO activity_logs (room_id, user_id, action, module, reference_id, description, created_at, updated_at) VALUES
  (1, 1, 'Room created',      'ROOM',    1, 'Aditi Sharma created room "Sunrise PG - Flat 4B"',            @now, @now),
  (1, 1, 'Member added',      'MEMBER',  2, 'Aditi Sharma added Rahul Verma to the room',                  @now, @now),
  (1, 2, 'Expense added',     'EXPENSE', 1, 'Rahul Verma added expense "Grocery Run" for 2000.00',         @now, @now),
  (1, 1, 'Expense approved',  'EXPENSE', 1, 'Aditi Sharma approved expense "Grocery Run"',                 @now, @now);

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
-- Default development credentials (all accounts use the same password)
-- =====================================================================
--  Admin:      admin@roommates.dev / password123
--  Member:     rahul@roommates.dev / password123
--  Member:     akash@roommates.dev / password123
--  Moderator:  priya@roommates.dev / password123
-- =====================================================================
